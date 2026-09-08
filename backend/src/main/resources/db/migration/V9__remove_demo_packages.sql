-- Production database: remove all DEMO seed packages and related bookings.
-- Real packages created via admin remain untouched.

DELETE FROM trip_bookings
WHERE booking_id IN (
    SELECT b.id
    FROM bookings b
    INNER JOIN packages p ON p.id = b.package_id
    WHERE p.slug LIKE '%-demo'
       OR p.title LIKE '%(DEMO)%'
       OR p.cancellation_policy LIKE '%(DEMO policy)%'
);

DELETE FROM payments
WHERE booking_id IN (
    SELECT b.id
    FROM bookings b
    INNER JOIN packages p ON p.id = b.package_id
    WHERE p.slug LIKE '%-demo'
       OR p.title LIKE '%(DEMO)%'
       OR p.cancellation_policy LIKE '%(DEMO policy)%'
);

DELETE FROM bookings
WHERE package_id IN (
    SELECT id FROM packages
    WHERE slug LIKE '%-demo'
       OR title LIKE '%(DEMO)%'
       OR cancellation_policy LIKE '%(DEMO policy)%'
);

DELETE FROM packages
WHERE slug LIKE '%-demo'
   OR title LIKE '%(DEMO)%'
   OR cancellation_policy LIKE '%(DEMO policy)%';

-- Drop legacy third-party or static frontend image URLs; Azure keys resolve at read time.
UPDATE package_images
SET image_url = NULL
WHERE image_url LIKE '%unsplash%'
   OR image_url LIKE '/images/%';
