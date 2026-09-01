-- Replace third-party Unsplash URLs with local frontend assets in /public/images

UPDATE package_images pi
SET image_url = '/images/mountains-lake.png'
FROM packages p
WHERE pi.package_id = p.id
  AND p.slug = 'nainital-weekend-escape-demo'
  AND pi.sort_order = 0
  AND pi.image_url LIKE '%unsplash%';

UPDATE package_images pi
SET image_url = '/images/mountains-hero.png'
FROM packages p
WHERE pi.package_id = p.id
  AND p.slug = 'nainital-weekend-escape-demo'
  AND pi.sort_order = 1
  AND pi.image_url LIKE '%unsplash%';

UPDATE package_images pi
SET image_url = '/images/mountains-peaks.png'
FROM packages p
WHERE pi.package_id = p.id
  AND p.slug = 'mussoorie-hill-retreat-demo'
  AND pi.sort_order = 0
  AND pi.image_url LIKE '%unsplash%';

UPDATE package_images pi
SET image_url = '/images/mountains-road.png'
FROM packages p
WHERE pi.package_id = p.id
  AND p.slug = 'mussoorie-hill-retreat-demo'
  AND pi.sort_order = 1
  AND pi.image_url LIKE '%unsplash%';

UPDATE package_images pi
SET image_url = '/images/mountains-road.png'
FROM packages p
WHERE pi.package_id = p.id
  AND p.slug = 'rishikesh-adventure-yoga-demo'
  AND pi.sort_order = 0
  AND pi.image_url LIKE '%unsplash%';

UPDATE package_images pi
SET image_url = '/images/mountains-lake.png'
FROM packages p
WHERE pi.package_id = p.id
  AND p.slug = 'rishikesh-adventure-yoga-demo'
  AND pi.sort_order = 1
  AND pi.image_url LIKE '%unsplash%';

UPDATE package_images pi
SET image_url = '/images/mountains-lake.png'
FROM packages p
WHERE pi.package_id = p.id
  AND p.slug = 'haridwar-spiritual-journey-demo'
  AND pi.sort_order = 0
  AND pi.image_url LIKE '%unsplash%';

UPDATE package_images pi
SET image_url = '/images/mountains-hero.png'
FROM packages p
WHERE pi.package_id = p.id
  AND p.slug = 'haridwar-spiritual-journey-demo'
  AND pi.sort_order = 1
  AND pi.image_url LIKE '%unsplash%';

UPDATE package_images pi
SET image_url = '/images/mountains-peaks.png'
FROM packages p
WHERE pi.package_id = p.id
  AND p.slug = 'dalhousie-snow-escape-demo'
  AND pi.sort_order = 0
  AND pi.image_url LIKE '%unsplash%';

UPDATE package_images pi
SET image_url = '/images/mountains-peaks.png'
FROM packages p
WHERE pi.package_id = p.id
  AND p.slug = 'dalhousie-snow-escape-demo'
  AND pi.sort_order = 1
  AND pi.image_url LIKE '%unsplash%';

-- Catch any remaining third-party demo URLs
UPDATE package_images
SET image_url = '/images/mountains-hero.png'
WHERE image_url LIKE '%unsplash%';
