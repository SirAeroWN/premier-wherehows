##Creating Distributable zips

bamboo should be handling this

##Setting up the MySQL

First, create the Database and users like so:

```
mysql -u root <<< "CREATE DATABASE wherehows DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;"
mysql -u root <<< "GRANT ALL ON wherehows.* TO 'wherehows'@'localhost' IDENTIFIED BY 'wherehows';"
mysql -u root <<< "GRANT ALL ON wherehows.* TO 'wherehows'@'%' IDENTIFIED BY 'wherehows';"
```

You may need to adjust this for differing versions of MySQL

Once per MySQL instance (so once for devi, once for stage, etc) the timezones need to be set up/fixed:

`mysql_tzinfo_to_sql /usr/share/zoneinfo | mysql -u root mysql`

Next, grab the `WhereHows/data-model/DDL` directory out of the `wherehows-fork` repo. Copy this folder onto one of the nodes (ex: `c3duslar1m`) then `ssh` onto that node and `cd` into the `DDL/` directory and run:

`mysql -uwherehows -pwherehows -Dwherehows < create_all_tables_wrapper.sql`

This should set up all the required tables and fill them with default preferences and data.

##Deploying Front End

Use the deploy.sh script in `cluster-templates/marathon/`, an example run used on devi with a local `cluster-templates` build:

```
./marathon/deploy.sh -l whz-web -p socks://localhost:2004 --verbose --local -v 1.4.6-SNAPSHOT -- devi
```

##Deploying Backend

Use the deploy.sh script in `cluster-templates/marathon/`, an example run used on devi with a local `cluster-templates` build:

```
./marathon/deploy.sh -l whz-backend -p socks://localhost:2004 --verbose --local -v 1.4.6-SNAPSHOT -- devi
```