CREATE TABLE post_table(
  post_id character varying(35) NOT NULL,
  created_at timestamp with time zone,
  from_id bigint,
  message text,
  post_type character varying(30),
  post_status_type character varying(40),
  to_user_id text,
  story text,
  CONSTRAINT post_table_pkey PRIMARY KEY (post_id));


CREATE TABLE page_fans(
  user_id character varying(35) NOT NULL,
  name text,
  type character varying(10),
  CONSTRAINT page_fans_pkey PRIMARY KEY (user_id));


CREATE TABLE post_counts_table(
  post_id character varying(35) NOT NULL,
  ts_post timestamp without time zone NOT NULL,
  shares_count integer,
  likes_count integer,
  comments_count integer,
  CONSTRAINT post_time PRIMARY KEY (post_id, ts_post),
  CONSTRAINT post_fk FOREIGN KEY (post_id)
      REFERENCES post_table (post_id));


CREATE TABLE post_likes_table(
  user_id character varying(35) NOT NULL,
  post_id character varying(35) NOT NULL,
  pl_ts timestamp without time zone,
  CONSTRAINT pl_pk PRIMARY KEY (user_id, post_id),
  CONSTRAINT pl_fk1 FOREIGN KEY (user_id)
      REFERENCES page_fans (user_id),
  CONSTRAINT pl_fk2 FOREIGN KEY (post_id)
      REFERENCES post_table (post_id));


CREATE TABLE comments_table (
  comment_id character varying(35) NOT NULL,
  post_id character varying(35),
  user_id character varying(35),
  message text,
  created_at timestamp with time zone,
  CONSTRAINT comments_table_pkey PRIMARY KEY (comment_id),
  CONSTRAINT comment_fk1 FOREIGN KEY (post_id)
      REFERENCES post_table (post_id),
  CONSTRAINT comment_fk2 FOREIGN KEY (user_id)
      REFERENCES page_fans (user_id);


CREATE TABLE comment_count_table (
  comment_id character varying(35) NOT NULL,
  ts_comment timestamp without time zone NOT NULL,
  like_count integer,
  reply_count integer,
  CONSTRAINT cct_pk PRIMARY KEY (comment_id, ts_comment),
  CONSTRAINT cct_fk FOREIGN KEY (comment_id)
      REFERENCES comments_table (comment_id));


CREATE TABLE comment_likes_table(
  user_id character varying(35) NOT NULL,
  comment_id character varying(35) NOT NULL,
  ts_comment_like timestamp without time zone,
  CONSTRAINT clt_pk PRIMARY KEY (user_id, comment_id),
  CONSTRAINT clt_fk1 FOREIGN KEY (user_id)
      REFERENCES page_fans (user_id),
  CONSTRAINT clt_fk2 FOREIGN KEY (comment_id)
      REFERENCES comments_table (comment_id));


CREATE TABLE reply_table(
  reply_id character varying(35) NOT NULL,
  comment_id character varying(35),
  user_id character varying(35),
  message text,
  created_at timestamp with time zone,
  CONSTRAINT reply_table_pkey PRIMARY KEY (reply_id),
  CONSTRAINT rt_fk1 FOREIGN KEY (comment_id)
      REFERENCES comments_table (comment_id),
  CONSTRAINT rt_fk2 FOREIGN KEY (user_id)
      REFERENCES page_fans (user_id));


CREATE TABLE reply_count_table(
  reply_id character varying(35) NOT NULL,
  ts_reply timestamp without time zone NOT NULL,
  like_count integer,
  CONSTRAINT rct_pk PRIMARY KEY (reply_id, ts_reply),
  CONSTRAINT rct_fk FOREIGN KEY (reply_id)
      REFERENCES reply_table (reply_id));


CREATE TABLE reply_likes_table(
  user_id character varying(35) NOT NULL,
  reply_id character varying(35) NOT NULL,
  ts_reply_like timestamp without time zone,
  CONSTRAINT rlt_pk PRIMARY KEY (user_id, reply_id),
  CONSTRAINT rlt_fk1 FOREIGN KEY (user_id)
      REFERENCES page_fans (user_id),
  CONSTRAINT rlt_fk2 FOREIGN KEY (reply_id)
      REFERENCES reply_table (reply_id));