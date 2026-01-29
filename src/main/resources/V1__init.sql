-- UUIDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE end_users (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL,
  CONSTRAINT uk_end_users_name UNIQUE (name)
);

CREATE TABLE drones (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL,
  status TEXT NOT NULL,
  last_lat DOUBLE PRECISION,
  last_lng DOUBLE PRECISION,
  last_heartbeat_at TIMESTAMPTZ,
  current_job_id UUID,
  CONSTRAINT uk_drones_name UNIQUE (name)
);

CREATE TABLE orders (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_by_end_user_id UUID NOT NULL,
  origin_lat DOUBLE PRECISION NOT NULL,
  origin_lng DOUBLE PRECISION NOT NULL,
  destination_lat DOUBLE PRECISION NOT NULL,
  destination_lng DOUBLE PRECISION NOT NULL,
  status TEXT NOT NULL,
  current_job_id UUID,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_orders_end_user FOREIGN KEY (created_by_end_user_id) REFERENCES end_users(id)
);

CREATE TABLE jobs (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  version BIGINT NOT NULL DEFAULT 0,
  order_id UUID NOT NULL,
  type TEXT NOT NULL,
  status TEXT NOT NULL,
  pickup_lat DOUBLE PRECISION NOT NULL,
  pickup_lng DOUBLE PRECISION NOT NULL,
  dropoff_lat DOUBLE PRECISION NOT NULL,
  dropoff_lng DOUBLE PRECISION NOT NULL,
  assigned_drone_id UUID,
  excluded_drone_id UUID,
  reserved_at TIMESTAMPTZ,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  failed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_jobs_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_order_id ON jobs(order_id);
