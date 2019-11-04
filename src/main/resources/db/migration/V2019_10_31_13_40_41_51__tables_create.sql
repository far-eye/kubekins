CREATE TABLE cluster
(
  id SERIAL PRIMARY KEY NOT NULL,
  project_id character varying(64),
  cluster_name character varying(64),
  region character varying(64),
  cluster_description character varying(256),
  domain_name character varying(256),
  created_by character varying(64),
  created_date timestamp without time zone,
  CONSTRAINT name_ukey UNIQUE (cluster_name)
);

CREATE TABLE micro_service_environment
(
    id SERIAL PRIMARY KEY NOT NULL,
    creation_date timestamp without time zone,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(16) NOT NULL,
    status varchar(50),
    logs varchar(1024),
    version varchar(16),
    autoscale Boolean,
    target_pods integer,
    minimum_pods integer,
    maximum_pods integer ,
    cpu_utilization integer,
    keep_environment_data_after_deletion boolean default false,
    environment_variables text,
    disk_size integer default 10,
    micro_service_id integer not null,
    UNIQUE (code)
);

CREATE TABLE micro_service
(
    id SERIAL PRIMARY KEY NOT NULL,
    creation_date timestamp without time zone,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(16) NOT NULL,
    git_url VARCHAR(2083) NOT NULL,
    number_of_environments integer default 0,
    UNIQUE (code)
);
ALTER TABLE micro_service_environment ADD COLUMN allow_new_relic boolean;
ALTER TABLE micro_service_environment ADD COLUMN node_affinity text;
ALTER TABLE micro_service_environment
    ADD COLUMN  old_version varchar(16),
    ADD COLUMN  last_modified_date timestamp without time zone;
ALTER TABLE micro_service_environment ADD COLUMN cluster_id integer;