-- Package images live in Azure Blob Storage. The database stores storage keys only.

-- Remove demo rows that stored static frontend paths or third-party URLs without blob keys.
DELETE FROM package_images
WHERE private_storage_key IS NULL
  AND (
    image_url LIKE '/images/%'
    OR image_url LIKE '%unsplash%'
  );

-- Stop persisting resolved URLs when a blob key exists (URLs are resolved at read time).
UPDATE package_images
SET image_url = NULL
WHERE private_storage_key IS NOT NULL;
