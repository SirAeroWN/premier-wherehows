#IGNORE THIS README FOR NOW
# WhereHows

WhereHows is a data discovery and lineage tool built at LinkedIn. It integrates with all the major data processing systems and collects both catalog and operational metadata from them. 

Within the central metadata repository, WhereHows curates, associates, and surfaces the metadata information through two interfaces: 
* a web application that enables data & linage discovery, and community collaboration
* an API endpoint that empowers automation of data processes/applications 

WhereHows serves as the single platform that:
* links data objects with people and processes
* enables crowdsourcing for data knowledge
* provides data governance and provenance based on ownership and lineage


## Getting Started


### Preparation

First, please get Play Framework in place.

    wget http://downloads.typesafe.com/play/2.2.4/play-2.2.4.zip

    # Unzip, Remove zipped folder, move play folder to $HOME
    unzip play-2.2.4.zip && rm play-2.2.4.zip && mv play-2.2.4 $HOME/

    # Add PLAY_HOME, GRADLE_HOME. Update Path to include new gradle, alias to counteract issues
    echo 'export PLAY_HOME="$HOME/play-2.2.4"' >> ~/.bashrc
    source ~/.bashrc



### Build

1. Clone the wherehows-fork repo from HGPG
2. Go back to the **WhereHows** root directory and build all the modules: `sudo -u ubuntu PLAY_HOME="/opt/play-2.2.4" SBT_OPTS="-Xms1G -Xmx2G -Xss16M" PLAY_OPTS="-Xms1G -Xmx2G -Xss16M"  ./gradlew build`
3. Set up the SQL tables by running:

        sudo mysql_tzinfo_to_sql /usr/share/zoneinfo | mysql -u root mysql
        sudo mysql -u root <<< "CREATE DATABASE wherehows DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;"
        sudo mysql -u root <<< "CREATE USER 'wherehows';"
        sudo mysql -u root <<< "SET PASSWORD FOR 'wherehows' = PASSWORD('wherehows');"
        sudo mysql -u root <<< "GRANT ALL ON wherehows.* TO 'wherehows';"
        sudo mysql -u root <<< "CREATE USER 'wherehows_ro';"
        sudo mysql -u root <<< "GRANT ALL ON wherehows.* TO 'wherehows_ro'"
        sudo mysql -u root <<< "SET PASSWORD FOR 'wherehows_ro' = PASSWORD('readmetadata');"
        sudo mysql -u root <<< "SET GLOBAL sql_mode=(SELECT REPLACE(@@sql_mode,'ONLY_FULL_GROUP_BY',''));"
        sudo mysql -uwherehows -pwherehows -Dwherehows < /opt/WhereHows/data-model/DDL/create_all_tables_wrapper.sql
        sudo mysql -uwherehows -pwherehows -Dwherehows < /opt/WhereHows/data-model/DDL/default_properties.sql
    
4. Go back to the **WhereHows** root directory and start the metadata ETL and API service: 

        ./backend-service/target/universal/stage/bin/backend-service -Dhttp.port=19001
        
   To start in the background instead, you can use `nohup`:
   
   		 nohup ./target/universal/stage/bin/backend-service -Dhttp.port=19001 > $LOG_PATH/backend-service.log 2>&1& 
        
5. Go back to the **WhereHows** root directory and start the web front-end: 

        ./target/universal/stage/bin/wherehows -Dhttp.port=9000
  
   Or run in the background with `nohup`:
   
        nohup ./target/universal/stage/bin/wherehows -Dhttp.port=9000 > $LOG_PATH/web.log 2>&1&
        
   Then WhereHows UI is available at `http://localhost:9000` by default.


 
## Documentation
