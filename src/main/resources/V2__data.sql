INSERT INTO end_user (id, name)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'test-enduser-1'),
  ('22222222-2222-2222-2222-222222222222', 'test-enduser-2');

INSERT INTO drone (id, name, status, last_lat, last_lng, last_heartbeat_at)
VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'drone-alpha', 'ACTIVE', 24.7136, 46.6753, CURRENT_TIMESTAMP),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'drone-bravo', 'ACTIVE', 24.7743, 46.7386, CURRENT_TIMESTAMP);

INSERT INTO delivery_order (
  id,
  created_by_end_user_id,
  origin_lat,
  origin_lng,
  destination_lat,
  destination_lng,
  status,
  current_job_id,
  created_at
)
VALUES
  (
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    24.7136,
    46.6753,
    24.7743,
    46.7386,
    'SUBMITTED',
    '44444444-4444-4444-4444-444444444444',
    CURRENT_TIMESTAMP
  ),
  (
    '55555555-5555-5555-5555-555555555555',
    '22222222-2222-2222-2222-222222222222',
    24.7136,
    46.6753,
    24.7743,
    46.7386,
    'IN_DELIVERY',
    '66666666-6666-6666-6666-666666666666',
    CURRENT_TIMESTAMP
  );

INSERT INTO job (
  id,
  version,
  order_id,
  type,
  status,
  pickup_lat,
  pickup_lng,
  dropoff_lat,
  dropoff_lng,
  assigned_drone_id,
  excluded_drone_id,
  reserved_at,
  started_at,
  completed_at,
  failed_at,
  created_at
)
VALUES
  (
    '44444444-4444-4444-4444-444444444444',
    0,
    '33333333-3333-3333-3333-333333333333',
    'PICKUP_AND_DELIVER',
    'OPEN',
    24.7136,
    46.6753,
    24.7743,
    46.7386,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    CURRENT_TIMESTAMP
  ),
  (
    '66666666-6666-6666-6666-666666666666',
    0,
    '55555555-5555-5555-5555-555555555555',
    'PICKUP_AND_DELIVER',
    'IN_PROGRESS',
    24.7136,
    46.6753,
    24.7743,
    46.7386,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    CURRENT_TIMESTAMP
  );
