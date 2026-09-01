-- Demo seed packages must not be bookable in production.

UPDATE packages
SET status = 'DRAFT'
WHERE slug LIKE '%-demo'
   OR cancellation_policy LIKE '%(DEMO policy)%';
