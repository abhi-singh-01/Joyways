# Azure Blob Storage — JOYWAY Phase 13

**Setup guide:** see [AZURE_STORAGE_SETUP.md](./AZURE_STORAGE_SETUP.md) for Azure Portal steps, RBAC, and production checklist.

This document describes how JOYWAY stores uploaded documents and package images, how to configure Azure Blob Storage for production, and how to migrate existing local files.

## Architecture

```
Upload (admin / booking)          Public read (package images only)
        |                                    |
        v                                    v
  DocumentStorageService  <--------  PublicFileController
        |
   +----+----+
   |         |
 local     azure
   |         |
 ./storage  joyway-documents (private container)
```

Business code depends on `DocumentStorageService` only. The active implementation is selected at startup:

| `STORAGE_PROVIDER` | Implementation | Use case |
|--------------------|----------------|----------|
| `local` (default) | `LocalDocumentStorageService` | Development, tests |
| `azure` | `AzureBlobStorageService` | Production |

Production startup **fails fast** if `STORAGE_PROVIDER` is not `azure` (`ProductionSecurityValidator`).

## Object layout

All objects use keys `{category}/{uuid}.{ext}`:

| Category | Visibility | Example key |
|----------|------------|-------------|
| `package-images` | Public via `/api/files/package-images/{filename}` | `package-images/a1b2c3.jpg` |
| `traveller-documents` | Private (admin only; no public endpoint) | `traveller-documents/d4e5f6.pdf` |

Validation (all providers):

- Allowed types: JPEG, PNG, WebP, PDF
- Max size: 5 MB
- Content-Type allowlist + magic-byte verification
- UUID filenames on new uploads
- Path traversal blocked on keys and categories

## Environment variables

### Development (`STORAGE_PROVIDER=local`)

```env
STORAGE_PROVIDER=local
STORAGE_LOCAL_PATH=./storage
```

Azure variables are ignored when the provider is `local`.

### Production (`STORAGE_PROVIDER=azure`)

```env
STORAGE_PROVIDER=azure
STORAGE_ENDPOINT=https://<account>.blob.core.windows.net
STORAGE_CONTAINER=joyway-documents
STORAGE_USE_MANAGED_IDENTITY=true
```

**Managed Identity (recommended):** assign the app identity **Storage Blob Data Contributor** on the storage account or container. No account keys in environment variables.

**Shared-key fallback (temporary only):**

```env
STORAGE_USE_MANAGED_IDENTITY=false
STORAGE_ACCESS_KEY=<storage-account-name>
STORAGE_SECRET_KEY=<account-key>
```

Legacy `STORAGE_BUCKET` is supported and maps to the container name when `STORAGE_CONTAINER` is unset.

## Azure setup checklist

1. Create a Storage Account (Standard, LRS is sufficient to start).
2. Create a **private** container named `joyway-documents`.
3. Disable anonymous public access on the account.
4. Enable HTTPS-only traffic.
5. Assign RBAC to the application identity:
   - Role: **Storage Blob Data Contributor**
   - Scope: container `joyway-documents` (least privilege)
6. Set production environment variables (see `backend/.env.production.example`).
7. Deploy backend with `SPRING_PROFILES_ACTIVE=prod`.

Do **not** embed connection strings or account keys in the frontend, Docker image layers, or repository.

## Authentication

| Mode | Configuration | Notes |
|------|---------------|-------|
| Managed Identity | `STORAGE_USE_MANAGED_IDENTITY=true` | Uses `DefaultAzureCredential` (works on App Service, Container Apps, AKS, local Azure CLI login for ops) |
| Shared key | `STORAGE_USE_MANAGED_IDENTITY=false` + access/secret key | Supported as fallback; rotate keys after moving to MI |

## Serving files

- **Package images:** still served through the backend at `{PUBLIC_API_BASE_URL}/api/files/package-images/{filename}`. Works with both local and Azure storage.
- **Traveller documents:** stored privately; stream via admin APIs using `DocumentStorageService.retrieve()` (no public URL).
- **SAS URLs:** `getReadAccessUrl()` returns short-lived read SAS when using shared-key auth. With Managed Identity, return empty and stream through the backend (user-delegation SAS can be added later).

## One-off migration (local → Azure)

Migration does **not** run on normal startup. Run manually when cut over to Azure:

1. Mount or copy existing `./storage` content to a path reachable by the app (e.g. `/app/storage`).
2. Configure Azure storage (`STORAGE_PROVIDER=azure`, endpoint, MI or keys).
3. Start once with the extra profile:

```bash
SPRING_PROFILES_ACTIVE=prod,storage-migration
```

4. Review logs for `event=storage.migration.complete`.
5. Remove `storage-migration` from profiles after success.

The migration preserves object keys, so **database rows do not need updating** when keys already follow `category/uuid.ext`.

Re-run is safe: existing blobs are skipped (`exists` check).

## Monitoring and operations

Log events (no secrets):

- `event=storage.uploaded provider=local|azure`
- `event=storage.deleted provider=...`
- `event=storage.migration.*`

In Azure Portal:

- Monitor **Availability**, **Success E2E Latency**, **Transactions** on the storage account
- Alert on repeated `storage.upload.failed` / `storage.download.failed` in application logs

## Cost notes

- Charges depend on stored GB, transactions, and egress.
- Package images are read on every listing/detail view; consider CDN in a future phase.
- Private traveller documents have lower read volume.

## Troubleshooting

| Symptom | Likely cause | Action |
|---------|--------------|--------|
| App fails at startup in prod | `STORAGE_PROVIDER` not `azure` | Set provider and endpoint |
| `403` on upload/download | Missing RBAC for MI | Grant Storage Blob Data Contributor |
| `STORAGE_SECRET_KEY is required` | MI false but keys missing | Enable MI or set keys |
| Images 404 after migration | Keys differ from DB | Re-run migration; verify keys in DB match blob names |
| Local files still used in prod | Wrong provider | Confirm env in deployment platform, not only compose file |

## Security

- Container remains private; public access only through controlled backend routes.
- Upload validation is shared across providers (`StorageUploadValidator` plus `UploadMalwareScanner`).
- Keys validated against traversal (`StorageKeyValidator`).
- Managed Identity read URLs use user-delegation SAS when the storage client is available.

## Related files

- `DocumentStorageService`, `LocalDocumentStorageService`, `AzureBlobStorageService`
- `StorageConfiguration` — provider wiring
- `ProductionSecurityValidator` — production fail-fast
- `LocalStorageMigrationRunner` — optional migration (`storage-migration` profile)
- `backend/.env.example`, `backend/.env.production.example`
- `docker-compose.prod.yml` — Azure storage env vars
