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
<a href="#new-backend-api">New Backend API</a>

<a href="#preferences">Preferences</a>



###<a name="new-backend-api">New Backend API</a>
1. <a href="#latest-get">Latest GET</a>
2. <a href="#latest-after-get">Latest After GET</a>
3. <a href="#latest-before-get">Latest Before GET</a>
4. <a href="#latest-between-get">Latest Between GET</a>
5. <a href="#latest-in-window-get">Latest in Window GET</a>
6. <a href="#at-time-get">At Time GET</a>
7. <a href="#change-properties-put">Change Properties PUT</a>
8. <a href="#change-validity-put">Change Validity PUT</a>
9. <a href="#property-get">Property GET</a>
10. <a href="#property-assignment-post">Property Assignment POST</a>
11. <a href="#property-assignment-put">Property Assignment PUT</a>
12. <a href="#tool-tip-list-get">Tool Tip List GET</a>
13. <a href="#tool-tip-list-post">Tool Tip List POST</a>
14. <a href="#tool-tip-list-put">Tool Tip List PUT</a>
15. <a href="#node-color-get">Node Color GET</a>
16. <a href="#node-color-post">Node Color POST</a>
17. <a href="#node-color-put">Node Color PUT</a>
18. <a href="#node-type-get">Node Type GET</a>
19. <a href="#node-type-post">Node Type POST</a>
20. <a href="#node-type-put">Node Type PUT</a>
21. <a href="#edge-style-get">Edge Style GET</a>
22. <a href="#edge-style-post">Edge Style POST</a>
23. <a href="#edge-style-put">Edge Style PUT</a>
24. <a href="#edge-type-get">Edge Type GET</a>
25. <a href="#edge-type-post">Edge Type POST</a>
26. <a href="#edge-type-put">Edge Type PUT</a>




####<a name="latest-get">Latest GET</a>
* **URL**

	/dataset/latest/:type

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| type        | scheme portion of urn | | Y |

* **Success Response:**

```json
{
	"urn": "domain-parquet:///share/domain/parquet"
}
```

* **Error Response:**

```json
{
	"message": "none found"
}
```

```
{
	"return_code": 400,
	"error_message": "type not provided"
}
```

```
{
	"message": "there was a problem"
}
``` 

* **Sample Call**

```
GET /dataset/latest/domain-parquet
```



####<a name="latest-after-get">Latest After GET</a>
* **URL**

	/dataset/after/:type/:time

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| type | scheme portion of urn | | Y |
| time | UNIX epoch time in seconds | | Y |

* **Success Response:**

```json
{
	"urn": "domain-parquet:///share/domain/parquet"
}
```


* **Error Response:**

```json
{
	"message": "none found"
}
```

```
{
	"return_code": 400,
	"error_message": "type not provided"
}
```

```
{
	"message": "there was a problem"
}
``` 


* **Sample Call**

```
GET /dataset/after/domain-parquet/1498830742
```



####<a name="latest-before-get">Latest Before GET</a>
* **URL**

	/dataset/before/:type/:time

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| type | scheme portion of urn | | Y |
| time | UNIX epoch time in seconds | | Y |

* **Success Response:**

```json
{
	"urn": "domain-parquet:///share/domain/parquet"
}
```


* **Error Response:**

```json
{
	"message": "none found"
}
```

```
{
	"return_code": 400,
	"error_message": "type not provided"
}
```

```
{
	"message": "there was a problem"
}
``` 


* **Sample Call**

```
GET /dataset/before/domain-parquet/1498830742
```



####<a name="latest-between-get">Latest Between GET</a>
* **URL**

	/dataset/between/:type/:firsttime/:secondtime

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| type | scheme portion of urn | | Y |
| firsttime | result time > firsttime; UNIX epoch time in seconds | | Y |
| secondtime | result time < secondtime; UNIX epoch time in seconds | | Y |


* **Success Response:**

```json
{
	"urn": "domain-parquet:///share/domain/parquet"
}
```


* **Error Response:**

```json
{
	"message": "none found"
}
```

```
{
	"return_code": 400,
	"error_message": "type not provided"
}
```

```
{
	"message": "there was a problem"
}
``` 


* **Sample Call**

```
GET /dataset/before/domain-parquet/1498830742/1498832147
```



####<a name="latest-in-window-get">Latest in Window GET</a>
* **URL**

	/dataset/attime/:type/:time/:window

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| type | scheme portion of urn | | Y |
| time | UNIX epoch time in seconds | | Y |
| window | seconds +- time | | Y |


* **Success Response:**

```json
{
	"urn": "domain-parquet:///share/domain/parquet"
}
```


* **Error Response:**

```json
{
	"message": "none found"
}
```

```
{
	"return_code": 400,
	"error_message": "type not provided"
}
```

```
{
	"message": "there was a problem"
}
``` 


* **Sample Call**

```
GET /dataset/attime/domain-parquet/1498830742/1000
```




####<a name="at-time-get">At Time GET</a>
* **URL**

	/dataset/attime/:type/:time

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| type | scheme portion of urn | | Y |
| time | UNIX epoch time in seconds | | Y |

* **Success Response:**

```json
{
	"urn": "domain-parquet:///share/domain/parquet"
}
```


* **Error Response:**

```json
{
	"message": "none found"
}
```

```
{
	"return_code": 400,
	"error_message": "type not provided"
}
```

```
{
	"message": "there was a problem"
}
``` 


* **Sample Call**

```
GET /dataset/attime/domain-parquet/1498830742
```





####<a name="change-properties-put">Change Properties PUT</a>
* **URL**

	/dataset/properties

* **Method:**

	`PUT`

* **Data Params**

```json
{
	"urn": "raw-parquet:///share/raw/parquet",
	"prop1": "value1",
	"prop2": 2,
	"propN": "valueN"
}
```
The json can include an unlimited number of properties, both new and old. If a property already exists then its value will be reassigned to whatever is passed in the call.

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "properties updated"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```


* **Sample Call**

```
PUT {"urn": "raw-parquet:///share/raw/parquet","prop1": "value1"} /dataset/properties
```





####<a name="change-validity-put">Change Validity PUT</a>
* **URL**

	/dataset/valid

* **Method:**

	`PUT`

* **Data Params**

```json
{
	"urn": "raw-parquet:///share/raw/parquet",
	"valid": "true" or "false"
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "validity updated"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```

```json
{
	"message": "there was a problem"
}
```


* **Sample Call**

```
PUT {"return_code": 200, "message": "validity updated"} /dataset/valid
```





####<a name="property-get">Property GET</a>
* **URL**

	/property/assigns/:name

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| name | urn scheme | | Y |

* **Success Response:**

```json
{
	"return_code": 200,
	"properties": /*comma delimited list of node properties*/
}
```

* **Error Response:**

```json
{
	"return_code": 200,
	"properties": "default"
}
```


* **Sample Call**

```
GET /property/assigns/raw-parquet
```




####<a name="property-assignment-post">Property Assignment POST</a>
* **URL**

	/property/assigns

* **Method:**

	`POST`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"properties": /*comma delimited list of node properties*/
}
```

```json
{
	"scheme": "raw-parquet",
	"properties": ["prop1", "prop2", ... , "propN"]
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Assignment Property inserted!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```


* **Sample Call**

```
POST {"scheme": "raw-parquet", "properties": "source_modified_time,prop/description"} /property/assigns
```




####<a name="property-assignment-put">Property Assignment PUT</a>
* **URL**

	/property/assigns

* **Method:**

	`PUT`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"properties": /*comma delimited list of node properties*/
}
```

```json
{
	"scheme": "raw-parquet",
	"properties": ["prop1", "prop2", ... , "propN"]
}
```
Note that this appends whatever is passed in the json and does not check if a property is already listed. Duplicates should not produce any errors when visualizing.

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Assignment Property updated!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```


* **Sample Call**

```
POST {"scheme": "raw-parquet", "properties": "source_modified_time,prop/description"} /property/assigns
```




####<a name="tool-tip-list-get">Tool Tip List GET</a>
* **URL**

	/property/sortlist/:name

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| name | urn scheme | | Y |

* **Success Response:**

```json
{
	"return_code": 200,
	"properties": /*comma delimited list of node tool tip properties*/
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```

```json
{
	"return_code": 200,
	"properties": "default"
}
```


* **Sample Call**

```
GET /property/sortlist/raw-parquet
```




####<a name="tool-tip-list-post">Tool Tip List POST</a>
* **URL**

	/property/sortlist

* **Method:**

	`POST`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"properties": /*comma delimited list of node properties*/
}
```

```json
{
	"scheme": "raw-parquet",
	"properties": ["prop1", "prop2", ... , "propN"]
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Assignment Property updated!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```


* **Sample Call**

```
POST {"scheme": "raw-parquet", "properties": "source_modified_time,prop/description"} /property/sortlist
```




####<a name="tool-tip-list-put">Tool Tip List PUT</a>
* **URL**

	/property/sortlist

* **Method:**

	`PUT`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"properties": /*comma delimited list of node properties*/
}
```

```json
{
	"scheme": "raw-parquet",
	"properties": ["prop1", "prop2", ... , "propN"]
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Assignment Property updated!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```


* **Sample Call**

```
PUT {"scheme": "raw-parquet", "properties": "source_modified_time,prop/description"} /property/sortlist
```




####<a name="node-color-get">Node Color GET</a>
* **URL**

	/property/node/color/:name

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| name | urn scheme | | Y |

* **Success Response:**

```json
{
	"return_code": 200,
	"color": "colorname" or "#FF019A"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
```

```json
{
	"return_code": 200,
	"color": "default"
}
```


* **Sample Call**

```
GET /property/node/color/raw-parquet
```



####<a name="node-color-post">Node Color POST</a>
* **URL**

	/property/node/color

* **Method:**

	`POST`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"color": "colorname" or "#FF019A"
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Node Color inserted!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```


* **Sample Call**

```
POST {"scheme": "raw-parquet", "color": "peachpuff"} /property/node/color
```




####<a name="node-color-put">Node Color PUT</a>
* **URL**

	/property/node/color

* **Method:**

	`PUT`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"color": "colorname" or "#FF019A"
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Node Color inserted!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```


* **Sample Call**

```
PUT {"scheme": "raw-parquet", "color": "peachpuff"} /property/node/color
```




####<a name="node-type-get">Node Type GET</a>
* **URL**

	/property/node/type/:name

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| name | urn scheme | | Y |

* **Success Response:**

```json
{
	"return_code": 200,
	"type": "data" or "app" or "db"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```

```json
{
	"return_code": 200,
	"error_message": "default"
}
```


* **Sample Call**

```
GET /property/node/type/raw-parquet
```




####<a name="node-type-post">Node Type POST</a>
* **URL**

	/property/node/type

* **Method:**

	`POST`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"type": "data" or "app" or "db"
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Node Type inserted!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```


* **Sample Call**

```
POST {"scheme": "raw-parquet", "type": "data"} /property/node/type
```




####<a name="node-type-put">Node Type PUT</a>
* **URL**

	/property/node/type

* **Method:**

	`PUT`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"type": "data" or "app" or "db"
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Node Type updated!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```


* **Sample Call**

```
PUT {"scheme": "raw-parquet", "type": "data"} /property/node/type
```




####<a name="edge-style-get">Edge Style GET</a>
* **URL**

	/property/edge/style/:name

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| name | urn scheme | | Y |

* **Success Response:**

```json
{
	"property_name": "edge.style." + name,
	"property_value": "an_edge_style"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```

```json
{
	"property_name": "edge.style." + name,
	"property_value": "default"
}
```


* **Sample Call**

```
GET /property/edge/style/raw-parquet
```




####<a name="edge-style-post">Edge Style POST</a>
* **URL**

	/property/edge/style

* **Method:**

	`POST`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"style": "style-string"
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Edge Style inserted!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message" /*java error message*/
}
```


* **Sample Call**

```
POST {"scheme":"from.db", "style":"stroke: #FF6600"} /property/edge/style
```




####<a name="edge-style-put">Edge Style PUT</a>
* **URL**

	/property/edge/style

* **Method:**

	`PUT`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"style": "style-string"
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Edge Style inserted!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message" /*java error message*/
}
```


* **Sample Call**

```
PUT {"scheme":"from.db", "style":"stroke: #FF6600"} /property/edge/style
```




####<a name="edge-type-get">Edge Type GET</a>
* **URL**

	/property/edge/type/:name

* **Method:**

	`GET`

* **Data Params**

| Param Names | Description | Default | Required |
| ----------- | ----------- | ------- |:--------:|
| name | urn scheme | | Y |


* **Success Response:**

```json
{
	"property_name": "edge.type." + name,
	"property_value": "an_edge_type"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message": /*java error message*/
}
```

```json
{
	"property_name": "edge.type." + name,
	"property_value": "default"
}
```


* **Sample Call**

```
GET /property/edge/type/raw-parquet
```




####<a name="edge-type-post">Edge Type POST</a>
* **URL**

	/property/edge/type

* **Method:**

	`POST`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"type": "type"
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Edge Type inserted!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message" /*java error message*/
}
```


* **Sample Call**

```
POST {"scheme":"between.prospector.druid", "type":"job"} /property/edge/type
```




####<a name="edge-type-put">Edge Type PUT</a>
* **URL**

	/property/edge/type

* **Method:**

	`PUT`

* **Data Params**

```json
{
	"scheme": "raw-parquet",
	"type": "type"
}
```

* **Success Response:**

```json
{
	"return_code": 200,
	"message": "Edge Type inserted!"
}
```

* **Error Response:**

```json
{
	"return_code": 400,
	"error_message" /*java error message*/
}
```


* **Sample Call**

```
PUT {"scheme":"between.prospector.druid", "type":"job"} /property/edge/type
```

###<a name="preferences">Preferences</a>
1. <a href="#node-color">Node Color</a>
2. <a href="#node-type">Node Type</a>
3. <a href="#node-assignments">Node Assignments</a>
4. <a href="#node-tool-tip">Node Tool Tip</a>
5. <a href="#edge-style">Edge Style</a>
6. <a href="#edge-type">Edge Type</a>
7. <a href="#edge-label">Edge Label</a>




<a name="edge-label">Node Color</a>


| Internal Prefix | Passed Prefix | Name | Value |
| --------------- | ------------- | ---- | ----- |
| node.color. | | scheme or type | [SVG color](http://www.graphviz.org/doc/info/colors.html#svg) or hex color



<a name="edge-label">Node Type</a>


| Internal Prefix | Passed Prefix | Name | Value |
| --------------- | ------------- | ---- | ----- |
| node.type. | | scheme | type |



<a name="edge-label">Node Assignments</a>


| Internal Prefix | Passed Prefix | Name | Value |
| --------------- | ------------- | ---- | ----- |
| prop. | | scheme or type | list of node attributes where prop/ references values in the properties field |



<a name="edge-label">Node Tool Tip</a>


| Internal Prefix | Passed Prefix | Name | Value |
| --------------- | ------------- | ---- | ----- |
| prop.sortlist | | scheme or type | list of node attributes to display in the tool tip; must be a subset of scheme or type's attributes |



<a name="edge-label">Edge Style</a>


| Internal Prefix | Passed Prefix | Name | Value |
| --------------- | ------------- | ---- | ----- |
| edge.style. | between. | scheme or name.scheme or name | Style strings are the same as used by [Dagre-D3](https://github.com/cpettitt/dagre-d3) |
| edge.style. | from. | scheme or name | Style strings are the same as used by [Dagre-D3](https://github.com/cpettitt/dagre-d3) |
| edge.style. | to. | scheme or name | Style strings are the same as used by [Dagre-D3](https://github.com/cpettitt/dagre-d3) |



<a name="edge-label">Edge Type</a>


| Internal Prefix | Passed Prefix | Name | Value |
| --------------- | ------------- | ---- | ----- |
| edge.type. | between. | scheme or name.scheme or name | data or app or db |
| edge.type. | from. | scheme or name | data or app or db |
| edge.type. | to. | scheme or name | data or app or db |



<a name="edge-label">Edge Label</a>


| Internal Prefix | Passed Prefix | Name | Value |
| --------------- | ------------- | ---- | ----- |
| edge.type. | between. | scheme or name.scheme or name | a string |
| edge.type. | from. | scheme or name | a string |
| edge.type. | to. | scheme or name | a string |