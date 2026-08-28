# Azure Storage Setup — JOYWAY

Step-by-step guide to configure Azure Blob Storage for JOYWAY production document and image storage.

**Do not put real credentials in this document.** Use placeholders and your platform secret store.

See also: [AZURE_BLOB_STORAGE.md](./AZURE_BLOB_STORAGE.md) for application architecture and migration details.

---

## 1. Create Azure Storage Account

In [Azure Portal](https://portal.azure.com):

1. **Create a resource** → **Storage account**
2. Subscription / resource group: match your JOYWAY production group
3. **Storage account name**: choose a globally unique name (this becomes `<storage-account-name>` below)
4. Region: same region as your backend when possible
5. Performance: **Standard**
6. Redundancy: **LRS** is sufficient to start (upgrade for DR later)
7. **Advanced → Security**:
   - Require secure transfer (HTTPS): **Enabled**
   - Allow blob anonymous access: **Disabled**
8. Create the account

Record the **Blob service endpoint**:

```text
https://<storage-account-name>.blob.core.windows.net
```

Do not invent this URL — copy it from the storage account **Endpoints** blade after creation.

---

## 2. Recommended storage account settings

| Setting | Recommendation |
|---------|----------------|
| Public blob access | **Disabled** (account level) |
| Minimum TLS | TLS 1.2 |
| Network | Start with public endpoint + firewall optional; restrict to backend outbound IPs when known |
| Soft delete (blobs) | Enable (7–30 days) for recovery |
| Versioning | Optional; useful for accidental overwrite recovery |
| Shared access keys | Disable key rotation only after Managed Identity is verified |

---

## 3. Create `joyway-documents` container

1. Open the storage account → **Containers**
2. **+ Container**
3. Name: `joyway-documents`
4. Public access level: **Private (no anonymous access)**
5. Create

---

## 4. Keep the container private

- Do **not** enable anonymous read/write on the container
- Do **not** make the storage account publicly writable
- Public package images are served through the JOYWAY backend (`/api/files/package-images/...`), not via anonymous blob URLs

---

## 5. Enable Managed Identity on the backend

On your hosting platform (Azure App Service, Container Apps, AKS, etc.):

1. Open the JOYWAY **backend** resource
2. **Identity** → **System assigned** → **On**
3. Save and note the **Object (principal) ID**

Managed Identity is the preferred production authentication path. The application uses `DefaultAzureCredential`, which resolves to the managed identity in Azure.

---

## 6. Assign Storage Blob Data Contributor

Minimum RBAC for upload / download / delete:

1. Storage account → **Access control (IAM)**
2. **Add role assignment**
3. Role: **Storage Blob Data Contributor**
4. Assign access to: **Managed identity**
5. Select your backend app’s managed identity
6. Scope: storage account or container `joyway-documents` (container scope is least privilege)

Do **not** assign **Owner** or **Contributor** at subscription scope for routine app operation.

---

## 7. Configure `STORAGE_PROVIDER`

Production **must** use Azure:

```env
STORAGE_PROVIDER=azure
```

Local development **must** use:

```env
STORAGE_PROVIDER=local
```

When `STORAGE_PROVIDER=local`, Azure variables are ignored and files are stored under `STORAGE_LOCAL_PATH` (default `./storage`).

---

## 8. Configure `STORAGE_ENDPOINT`

Production example (replace with your real endpoint):

```env
STORAGE_ENDPOINT=https://<storage-account-name>.blob.core.windows.net
STORAGE_CONTAINER=joyway-documents
STORAGE_USE_MANAGED_IDENTITY=true
```

Legacy compatibility (optional):

```env
STORAGE_BUCKET=joyway-documents
```

`STORAGE_BUCKET` maps to the same container when `STORAGE_CONTAINER` is unset.

---

## 9. Local development configuration

`backend/.env.example`:

```env
STORAGE_PROVIDER=local
STORAGE_LOCAL_PATH=./storage
```

No Azure credentials required. Docker Compose dev mounts `backend-storage` at `/app/storage` when using containerized local runs.

---

## 10. Production configuration

`backend/.env.production.example`:

```env
SPRING_PROFILES_ACTIVE=prod
STORAGE_PROVIDER=azure
STORAGE_ENDPOINT=https://<storage-account-name>.blob.core.windows.net
STORAGE_CONTAINER=joyway-documents
STORAGE_USE_MANAGED_IDENTITY=true
STORAGE_SAS_EXPIRY_MINUTES=15
```

**Temporary shared-key fallback** (remove after MI is verified):

```env
STORAGE_USE_MANAGED_IDENTITY=false
STORAGE_ACCESS_KEY=<storage-account-name>
STORAGE_SECRET_KEY=<account-key-from-portal>
```

Note: despite the name, `STORAGE_ACCESS_KEY` holds the **storage account name**, not an RBAC key. This naming is legacy; prefer Managed Identity.

Startup validation (`ProductionSecurityValidator` + Azure container probe) **fails fast** if:

- `STORAGE_PROVIDER` is not `azure` in prod
- `STORAGE_ENDPOINT` is missing or localhost
- Managed Identity is disabled but account keys are missing
- The container cannot be reached

---

## 11. Security

| Control | Implementation |
|---------|----------------|
| Private documents | `traveller-documents/*` — no public HTTP route; stream via authorized admin APIs only |
| Public package images | Served via backend `/api/files/package-images/{filename}` only |
| Object keys | `{category}/{uuid}.{ext}` — never user-supplied filenames |
| Upload validation | MIME + magic bytes + 5 MB max + extension allowlist |
| Path traversal | Blocked in `StorageKeyValidator` and `PublicFileController` |
| Credentials | Backend only; never `NEXT_PUBLIC_*`; never in Git; never in Docker image layers |
| SAS tokens | Short-lived optional helper only; not stored in database |
| Database | Stores stable keys (`privateStorageKey`, `privateDocumentStorageKey`), not secrets |

---

## 12. Migration from `./storage`

If production previously used local `./storage`:

1. **Do not delete** existing local files
2. Deploy with `STORAGE_PROVIDER=azure` and valid Azure config
3. Mount local files at `STORAGE_LOCAL_PATH` (e.g. `/app/storage`)
4. Run **once**:

   ```bash
   SPRING_PROFILES_ACTIVE=prod,storage-migration
   ```

5. Review logs: `event=storage.migration.complete`
6. Remove `storage-migration` profile from normal runs

Object keys are preserved (`package-images/{uuid}.jpg`, `traveller-documents/{uuid}.pdf`), so database rows typically **do not** need updating.

---

## 13. Backup and recovery

- Enable **blob soft delete** and optionally **versioning** on the storage account
- Database backups (Azure PostgreSQL) remain separate — they store keys, not file bytes
- Recovery procedure:
  1. Restore PostgreSQL to a point in time
  2. Ensure blob container still contains objects referenced by restored keys
  3. Re-run migration only for objects missing from blob storage

---

## 14. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Startup: `STORAGE_PROVIDER must be 'azure'` | Prod profile with local provider | Set `STORAGE_PROVIDER=azure` |
| Startup: `Azure Blob container not found` | Container name mismatch | Create `joyway-documents` or fix `STORAGE_CONTAINER` |
| Startup: `Unable to access Azure Blob Storage` | Wrong endpoint, firewall, or RBAC | Verify endpoint URL, MI role assignment, network rules |
| `403` on upload | MI lacks data plane role | Assign **Storage Blob Data Contributor** |
| Package image 404 | Blob missing or wrong key | Check `privateStorageKey` in DB matches blob name |
| App uses `./storage` in prod | Provider not set in deployment env | Fix platform env vars (not only local `.env`) |
| Docker build contains secrets | `.env` copied into image | Confirm `backend/.dockerignore` excludes `.env` |

---

## Manual steps still required (GO / NO-GO)

The application **does not** create Azure resources. Before production GO:

- [ ] Storage account created; real endpoint recorded
- [ ] Private container `joyway-documents` created
- [ ] Managed Identity enabled on backend host
- [ ] **Storage Blob Data Contributor** assigned
- [ ] Production env vars set on hosting platform
- [ ] End-to-end test: admin package image upload → public URL loads
- [ ] End-to-end test: booking document upload → admin retrieval (when exposed)
- [ ] Optional: migration run completed and verified

**STATUS: NO-GO** until the checklist above is completed with a real storage account and successful upload/download verification.
