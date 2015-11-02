# Schema
 
# --- !Ups

CREATE SEQUENCE user_id_seq;
CREATE TABLE user (
    id integer NOT NULL DEFAULT nextval('user_id_seq'),
    username varchar(255),
    password varchar(255),
    first_name varchar(255),
    last_name varchar(255)    
);

CREATE TABLE friends (
	first_id integer NOT NULL,
	second_id integer NOT NULL,
	status integer
);

CREATE SEQUENCE image_id_seq;
CREATE TABLE image (
	id integer NOT NULL DEFAULT nextval('image_id_seq'),
	name varchar(255),
	user integer,
	public integer
);

CREATE TABLE user_like (
	user integer,
	image integer
);

CREATE TABLE tagged (
	user integer,
	image integer
);

CREATE SEQUENCE comment_id_seq;
CREATE TABLE comment (
	id integer NOT NULL DEFAULT nextval('comment_id_seq'),
	user integer,
	image integer,
	text varchar(255)
); 

INSERT INTO user (username, password, first_name, last_name) VALUES('admin', '1234', 'admin', 'istrator');
INSERT INTO user (username, password, first_name, last_name) VALUES('zika', '1234', 'ivan', 'zikovic');
 
# --- !Downs
 
DROP SEQUENCE user_id_seq;
DROP SEQUENCE image_id_seq;
DROP SEQUENCE comment_id_seq;
DROP TABLE user;
DROP TABLE image;
DROP TABLE friends;
DROP TABLE user_like;
DROP TABLE tagged;
DROP TABLE comment;