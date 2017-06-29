var g;
var svg;
var l;
var rotation = 1;
var g_currentData;
var g_scale = 1;
var g_upLevel = 1;
var g_downLevel = 1;
(function ($) {
    var skipNodeTitles = ['id', 'source', 'target', 'sourceLinks', 'targetLinks', 'urn'];
    var rotation = false;
    var ZOOM_DURATION = 2000;
    function setupSearch() {
        $("#nodeInfoSplitter").tabs({ active: 0 });
        var width = $(window).width()*0.99 + 10;
        var height = ($(window).height() * 0.99) - 82;
        $('#mainSplitter').height(height);
        $('#graphSplitter').height(height*0.6);
        $('#nodeInfoSplitter').height(height*0.4);
        $("#mainSplitter").splitter({
            type: "h",
            dock: "bottom",
            dockSpeed: 200,
            dockKey: 'W',
            accessKey: 'W',
            sizeTop: true
        });

        refreshLineageData();

        $('#rotationgraphbtn').click(function (e) {
            if (g_currentData)
            {
                var type = $('#lineageType').val();
                rotation = !rotation;
                setupDagreGraph(g_currentData, rotation, type);
            }
        });

        $("#uplevelbtn").click(function(){
            g_upLevel++;
            refreshLineageData();
        });

        $("#downlevelbtn").click(function(){
            g_downLevel++;
            refreshLineageData();
        });
    }

    function refreshLineageData()
    {
        var type = $('#lineageType').val();
        var id = $('#lineageID').val();
        var application = $('#application').val();
        if (application)
        {
            application = application.replace(/\./g, " ");
        }
        var project = $('#project').val();
        var flow = $('#flow').val();
        var url = '';
        if (type == 'chains')
        {
            url = 'api/v1/lineage/chains';
            $("#chainComboBox").on('change', function (event) {
                var items = $("#chainComboBox").jqxComboBox('getSelectedItems');
                var names = '';
                $.each(items, function (index) {
                    names = names + this.label;
                    if (items.length - 1 != index) {
                        names += ",";
                    }
                });
                if (names)
                {
                    var dataUrl = 'api/v1/lineage/appworxflow/' + names;
                    $("#loading").show();
                    $.get(dataUrl, function(data) {
                        if (data && data.status == "ok"){
                            $("#loading").hide();
                            renderTables(data.data);
                            g_currentData = data.data;
                            setupDagreGraph(data.data, rotation, type);
                            $('#nodeInfoTab a:first').tab("show");
                        }
                    });
                }
            });
            $("#loading").show();
            $.get(url, function(data) {
                if (data && data.status == "ok"){
                    $("#chainComboBox").jqxComboBox({source: data.chains, multiSelect: true, width: 450, height: 25});
                    $("#chainComboBox").jqxComboBox('selectItem', 'TAGG_PV_PAGE_STATS');
                }
                $("#loading").hide();
            });
        }
        else if (type == 'dataset')
        {
            url = '/api/v1/lineage/dataset/' + id + '?upLevel=' + g_upLevel + '&downLevel=' + g_downLevel;
            $("#loading").show();
            $.get(url, function(data) {
                if (data && data.status == "ok"){
                    $("#loading").hide();
                    var titleObj = $('#title');
                    if (titleObj && data.data && data.data.urn)
                    {
                        titleObj.text("Lineage for: " + data.data.urn);
                    }
                    renderTables(data.data);
                    g_currentData = data.data;
                    setupDagreGraph(data.data, rotation, type);
                    $('#nodeInfoTab a:first').tab("show");
                }
            });
        }
        else if (type == 'metric')
        {
            url = '/api/v1/lineage/metric/' + id + '?upLevel=' + g_upLevel + '&downLevel=' + g_downLevel;;
            $("#loading").show();
            $.get(url, function(data) {
                if (data && data.status == "ok"){
                    $("#loading").hide();
                    renderTables(data.data);
                    g_currentData = data.data;
                    setupDagreGraph(data.data, rotation, type);
                    $('#nodeInfoTab a:first').tab("show");
                }
            });
        }
        else if (type == 'azkaban')
        {
            url = '/api/v1/lineage/flow/' + application + '/' + project + '/' + flow;
            $("#loading").show();
            $.get(url, function(data) {
                if (data && data.status == "ok"){
                    $("#loading").hide();
                    var titleObj = $('#title');
                    if (titleObj && data.data && data.data.flowName)
                    {
                        titleObj.text("Lineage for: " + application + '/' + project + '/' + data.data.flowName);
                    }
                    renderTables(data.data);
                    g_currentData = data.data;
                    setupDagreGraph(data.data, rotation, type);
                    $('#nodeInfoTab a:first').tab("show");
                }
            });
        }
        else if (type == 'appworx')
        {
            url = '/api/v1/lineage/flow/' + application + '/' + project + '/' + flow;
            $("#loading").show();
            $.get(url, function(data) {
                if (data && data.status == "ok"){
                    $("#loading").hide();
                    var titleObj = $('#title');
                    if (titleObj && data.data && data.data.flowName)
                    {
                        titleObj.text("Lineage for: " + application + '/' + project + '/' + data.data.flowName);
                    }
                    renderTables(data.data);
                    g_currentData = data.data;
                    setupDagreGraph(data.data, rotation, type);
                    $('#nodeInfoTab a:first').tab("show");
                }
            });
        }
    }

    function renderTables(data)
    {
        var dataTable = $('#lineagedatatable');
        var jobTable = $('#lineagejobtable');
        dataTable.html('');
        jobTable.html('');
        var firstDataNode = false;
        var firstJobNode = false;
        if (dataTable && data)
        {
            var nodes = data.nodes;
            if (nodes)
            {
                var dataHeader = '<thead>';
                var dataBody = '<tbody>';
                var jobHeader = '<thead>';
                var jobBody = '<tbody>';
                dataHeader += '<tr class="results-header wrap-all-word">';
                jobHeader += '<tr class="results-header wrap-all-word">';
                var dataHeaderNames = [];
                var jobHeaderNames = [];
                for(var i = 0; i < nodes.length; i++)
                {
                    dataBody += '<tr id="data-table-tr-' + nodes[i].id + '" class="result">';
                    jobBody += '<tr id="job-table-tr-' + nodes[i].id + '" class="result">';
                    if (nodes[i].node_type == 'data')
                    {
                        if (!firstDataNode)
                        {
                            if (nodes[i]["_sort_list"])
                            {
                                $.each(nodes[i]["_sort_list"], function(k, v) {

                                    dataHeader += '<th >' + v + '</th>';
                                    dataHeaderNames.push(v);
                                    if (nodes[i][v])
                                    {
                                        dataBody += '<td class="wrap-all-word">' + nodes[i][v] + '</td>';
                                    }
                                    else
                                    {
                                        dataBody += '<td class="wrap-all-word">' + '</td>';
                                    }
                                });
                            }
                            firstDataNode = true;
                        }
                        else
                        {
                            for (var j = 0; j < dataHeaderNames.length; j++)
                            {
                                if (nodes[i][dataHeaderNames[j]])
                                {
                                    dataBody += '<td class="wrap-all-word">' + nodes[i][dataHeaderNames[j]] + '</td>';
                                }
                                else
                                {
                                    dataBody += '<td class="wrap-all-word">' + '</td>';
                                }
                            }
                        }
                    }
                    else if (nodes[i].node_type == 'script')
                    {
                        if (!firstJobNode)
                        {
                            if (nodes[i]['_sort_list'])
                            {
                                $.each(nodes[i]['_sort_list'], function(k, v) {

                                    jobHeader += '<th >' + v + '</th>';
                                    jobHeaderNames.push(v);
                                    if (nodes[i][v])
                                    {
                                        jobBody += '<td class="wrap-all-word">' + nodes[i][v] + '</td>';
                                    }
                                    else
                                    {
                                        jobBody += '<td class="wrap-all-word">' + '</td>';
                                    }
                                });
                            }
                            firstJobNode = true;
                        }
                        else
                        {
                            for (var j = 0; j < jobHeaderNames.length; j++)
                            {
                                if (nodes[i][jobHeaderNames[j]])
                                {
                                    jobBody += '<td class="wrap-all-word">' + nodes[i][jobHeaderNames[j]] + '</td>';
                                }
                                else
                                {
                                    jobBody += '<td class="wrap-all-word">' + '</td>';
                                }
                            }
                        }

                    }
                    dataBody += '</tr>';
                    jobBody += '</tr>';
                }
                dataBody += '</tbody>';
                jobBody += '</tbody>';
                dataHeader += '</tr>';
                dataHeader +=  '</thead>';
                jobHeader += '</tr>';
                jobHeader +=  '</thead>';
                dataTable.append(dataHeader);
                dataTable.append(dataBody);
                jobTable.append(jobHeader);
                jobTable.append(jobBody);
            }
        }
    }

    function setViewport(scale, translation, duration) {
        if (!duration || duration < 0) { duration = 0; }
        d3.select('.graph-attach g')
            .transition()
            .duration(duration)
            .attr("transform","translate("+translation[0]+","+translation[1]+") scale("+1+")");
        //renderer.setOriginalScale(1);
        //minimapZoom(translation, 1);
    }

    function zoomTo(scale, focus, duration) {
        var width = $('#svg-canvas').width();
        var height = $('#svg-canvas').height();

        var translation = [
            -((focus[0] * scale) - (width  / 2)),
            -((focus[1] * scale) - (height / 2))
        ];

        return setViewport(scale, translation, duration);
    }

    function setupDagreGraph(data, rotation, type)
    {
        d3.select("svg")
            .remove();
        $('#canvas').html('<svg id="svg-canvas" width="1024"></svg>');
        var width = $(window).width()*0.99;
        $('#svg-canvas').width(width);
        $('#svg-canvas').height((($(window).height() * 0.99) - 82)*0.6 - 40);

        this.$container = $('#controls');
        $field  = this.$container.find('.search-field');
        this.$submit = this.$container.find('.search-submit');
        this.$clear  = this.$container.find('.search-clear');
        $results = $('<ul/>', { class: 'search-results' }).appendTo(this.$container);

        $field.keydown(_.debounce(onFieldKeydown, 200));
        this.$container.on('click', '.search-field, .search-submit, .search-clear', onSearchClick);
        $results.on('focus', 'li', onResultFocus);

        var SELECTED = 'result-selected';
        var KEYCODES = { up: 38, down: 40, enter: 13, esc: 27 };
        var MIN_QUERY_LENGTH = 3;
        var resultNodes = {};
        var selectedNodes = [];

        function getSelectedNodeData(){
            var selected = $results.children('.'+ SELECTED).get();

            return _.map(selected, function (el) {
                var id = $(el).data('node-id');
                return resultNodes[id];
            }, this);
        }

        function highlightNodesAndLinks(nodes, duration)
        {
            var graphNodes = d3.selectAll('.node');
            var graphLinks = d3.selectAll('.edgePath');

            for(var i in nodes)
            {
                for(var l in graphLinks[0])
                {
                    if (graphLinks[0][l] && graphLinks[0][l].__data__ && graphLinks[0][l].__data__.v)
                    {
                        for(var s in g_currentData.nodes[nodes[i].id].sourceLinks)
                        {
                            var link = g_currentData.nodes[nodes[i].id].sourceLinks[s];
                            if (graphLinks[0][l].__data__.v == link.source && graphLinks[0][l].__data__.w == link.target)
                            {
                                d3.select(graphLinks[0][l])
                                    .transition().duration(ZOOM_DURATION)
                                    .style("opacity", 1);
                                break;
                            }
                        }

                        for(var t in g_currentData.nodes[nodes[i].id].targetLinks)
                        {
                            var link = g_currentData.nodes[nodes[i].id].targetLinks[t];
                            if (graphLinks[0][l].__data__.v == link.source && graphLinks[0][l].__data__.w == link.target)
                            {
                                d3.select(graphLinks[0][l])
                                    .transition().duration(ZOOM_DURATION)
                                    .style("opacity", 1);
                                break;
                            }
                        }
                    }
                }
                d3.select(graphNodes[0][nodes[i].id])
                    .transition().duration(duration)
                    .style("opacity", 1);
            }

        }

        function zoomToNodes(nodes, duration){
            if (!nodes) { return };
            if (!_.isArray(nodes)) { nodes = [nodes]; } // Allow passing a single node
            if (!nodes.length) { return };

            if (nodes.length == 1)
            {
                selectTabularRow(nodes[0].id, nodes[0].id);
            }

            maskGraph(duration);
            var graphNodes = d3.selectAll('.node');
            var graphLinks = d3.selectAll('.edgePath');
            highlightNodesAndLinks(nodes, duration);

            var padding = {
                    left: 40, right: 80, // use more padding on bottom/right
                    top: 40, bottom: 80  // accommodate node width and height
                },
                left = _.min(_.map(nodes, function (node) {
                        var n = graphNodes[0][node.id];
                        var transformIndex = 0;
                        for(i = 0; i < n.attributes.length; i++)
                        {
                            if (n.attributes[i].name == 'transform')
                            {
                                transformIndex = i;
                                break;
                            }
                        }
                        var index = n.attributes[transformIndex].value.indexOf(',');
                        var x = parseFloat(n.attributes[transformIndex].value.substring(10, index));
                        var y = parseFloat(
                            n.attributes[transformIndex].value.substring(
                                index+1, n.attributes[transformIndex].value.length-1));
                        return x; })) - padding.left,
                right = _.max(_.map(nodes, function (node) {
                        var n = graphNodes[0][node.id];
                        var transformIndex = 0;
                        for(i = 0; i < n.attributes.length; i++)
                        {
                            if (n.attributes[i].name == 'transform')
                            {
                                transformIndex = i;
                                break;
                            }
                        }
                        var index = n.attributes[transformIndex].value.indexOf(',');
                        var x = parseFloat(n.attributes[transformIndex].value.substring(10, index));
                        var y = parseFloat(n.attributes[transformIndex].value.substring(
                            index+1,
                            n.attributes[transformIndex].value.length-1));
                        return x; })) + padding.right,
                top = _.min(_.map(nodes, function (node) {
                        var n = graphNodes[0][node.id];
                        var transformIndex = 0;
                        for(i = 0; i < n.attributes.length; i++)
                        {
                            if (n.attributes[i].name == 'transform')
                            {
                                transformIndex = i;
                                break;
                            }
                        }
                        var index = n.attributes[transformIndex].value.indexOf(',');
                        var x = parseFloat(n.attributes[transformIndex].value.substring(10, index));
                        var y = parseFloat(n.attributes[transformIndex].value.substring(
                            index+1,
                            n.attributes[transformIndex].value.length-1));
                        return y; })) - padding.top,
                bottom = _.max(_.map(nodes, function (node) {
                        var n = graphNodes[0][node.id];
                        var transformIndex = 0;
                        for(i = 0; i < n.attributes.length; i++)
                        {
                            if (n.attributes[i].name == 'transform')
                            {
                                transformIndex = i;
                                break;
                            }
                        }
                        var index = n.attributes[transformIndex].value.indexOf(',');
                        var x = parseFloat(n.attributes[transformIndex].value.substring(10, index));
                        var y = parseFloat(n.attributes[transformIndex].value.substring(
                            index+1,
                            n.attributes[transformIndex].value.length-1));
                        return y; })) + padding.bottom;

            var width = $('#svg-canvas').width();
            var height = $('#svg-canvas').height();
            var groupWidth = right - left,
                groupHeight = bottom - top,
                viewportRatio = width / height,
                groupRatio = groupWidth / groupHeight,
                groupCenter = [left, top],
                newScale = viewportRatio < groupRatio ? this.width / groupWidth : this.height / groupHeight;

            return zoomTo(1, groupCenter, duration);
        }

        function focusOnNodes(nodes){
            deselectAll();
            zoomToNodes(nodes, ZOOM_DURATION);
        }

        function onResultFocus(evt) {
            selectResultListItem($(this));
            focusOnNodes(getSelectedNodeData());
        }

        function selectResultListItem($result)
        {
            if ($result.hasClass(SELECTED)) {
                $result.removeClass(SELECTED);
            } else {
                this.$results.children().removeClass(SELECTED);
                $result.addClass(SELECTED);
            }
        }

        function onSearchClick(evt) {
            var $target = $(this);

            if ($target.hasClass('search-field')) {
                $results.children().removeClass(SELECTED);
            } else if ($target.hasClass('search-submit')) {
            } else if ($target.hasClass('search-clear')) {
                $field.val('');
                clearResults();
            }
        }

        $(document).on('keydown', function (evt) {
            if (evt.which === KEYCODES.esc){
                $results.fadeOut('fast', function(){
                    $results.empty();
                });
            }
        });

        $(document).on('keydown', '.search .search-field, .search .search-result', function (evt) {
            var $target = $(this),
                $toSelect;
            // On down arrow press (from search input), select first result

            if ($target.hasClass('search-field')) {
                if (evt.which === KEYCODES.down) {
                    $results.children(':first-child').focus();

                    evt.stopPropagation();
                    evt.preventDefault();
                }

                // On up/down arrow press (from search result li) select prev/next result
            } else if ($target.hasClass('search-result')) {
                if (evt.which === KEYCODES.down || evt.which === KEYCODES.up) {

                    if (evt.which === KEYCODES.up)   { $toSelect = $target.prev(); }
                    if (evt.which === KEYCODES.down) { $toSelect = $target.next(); }

                    if ($toSelect.length) {
                        $results.children().removeClass(SELECTED);
                        $toSelect.focus();
                    }

                    evt.preventDefault();
                    evt.stopPropagation();
                }
            }
        });

        function maskGraph(duration)
        {
            d3.selectAll(".edgePath")
                .transition().duration(duration)
                .style("opacity", 0.2);
            d3.selectAll(".edgeLabel")
                .transition().duration(duration)
                .style("opacity", 0.2);
            d3.selectAll(".node")
                .transition().duration(duration)
                .style("opacity", 0.2);
        }

        function clearGraph(duration)
        {
            d3.selectAll(".edgePath")
                .transition().duration(duration)
                .style("opacity", 1);
            d3.selectAll(".edgeLabel")
                .transition().duration(duration)
                .style("opacity", 1);
            d3.selectAll(".node")
                .transition().duration(duration)
                .style("opacity", 1);
        }

        function deselectAll()
        {
            selectedNodes = [];
            clearGraph(ZOOM_DURATION);
            d3.select('.graph-attach g').attr("transform","translate("+0+","+0+") scale("+originalMapScale+")");
        }

        function clearResults(nodes)
        {
            deselectAll();
            resultNodes = {};
            var $results = this.$results;
            $results.fadeOut('fast', function(){
                $results.empty();
            })
        }

        function graphSearch(nodes, query) {
            if (!query || !query.length) { return []; }

            function getMatches(nodes) {
                var matches = _(nodes).map(function (node) {
                    var text = '';
                    if (g_currentData.nodes[node.id].hasOwnProperty("script_name") && g_currentData.nodes[node.id]["script_name"])
                    {
                        text = g_currentData.nodes[node.id]["script_name"];
                    }

                    if (node.label.toLowerCase().indexOf(query.toLowerCase()) !== -1){
                        return node;
                    }
                    else if (text && text.toLowerCase().indexOf(query.toLowerCase()) !== -1){
                        return node;
                    }
                    else {
                        return null;
                    }
                });

                return matches.filter()  // filter out nulls
                    .flatten() // merge results from clusters
                    .value();  // extract array from Lodash object
            }
            return getMatches(nodes);
        }

        function performSearch()
        {
            deselectAll();
            var query = $field.val(),
                results = query.length >= MIN_QUERY_LENGTH ? graphSearch(g._nodes, query) : [];
            if (!results.length) {
                clearResults();
                graphSelect();
            } else {
                this.$results.empty();
                _.each(results, function (node) {
                    var text = node.label;
                    if (g_currentData.nodes[node.id].hasOwnProperty("script_name") && g_currentData.nodes[node.id]["script_name"])
                    {
                        text = g_currentData.nodes[node.id]["script_name"];
                    }

                    this.$results.append($('<li/>', {
                        'data-node-id': node.id,
                        'text': text,
                        'class': 'search-result',
                        'tabindex': 1
                    }));

                    resultNodes[node.id] = node;
                }, this);

                this.$results.fadeIn();
                graphSelect(results);
            }
        }

        function graphSelect(nodes)
        {
            if(!nodes){
                clearGraph(ZOOM_DURATION);
                return;
            }

            if (!_.isArray(nodes)) {
                nodes = [nodes]; // Allow passing a single node
            }
            if (nodes.length != 0)
            {
                maskGraph(ZOOM_DURATION);
                highlightNodesAndLinks(nodes, ZOOM_DURATION);
            }
            else
            {
                clearGraph(ZOOM_DURATION);
            }
        }

        function onFieldKeydown(evt) {
            if (!_.contains(KEYCODES, evt.which)) {
                performSearch();
            }
            if (evt.which === KEYCODES.enter) {
                //search.focusOnNodes(graph.selectedNodes);
            }
        }

        $('#search-clear').click(function(){
            $('#searchfield').val('');
            clearGraph(ZOOM_DURATION);
        });

        var g = new dagreD3.graphlib.Graph();
        if (rotation)
        {
            g.setGraph({rankdir: "TB"});
        }
        else
        {
            g.setGraph({rankdir: "LR"});
        }

        // this part is what needs to be customized
        g.setDefaultEdgeLabel(function() { return {}; });

        var styles = { 'LI_FLOW_START': 'fill:lightsteelblue', 'LI_FLOW_END' : 'fill:lightsteelblue',
            'LI_BTEQ' : 'fill:aquamarine', 'LINKEDIN_SHELL_PARAM' : 'fill:lightcyan',
            'LI_INFA' : 'fill:lightpink', 'LINKEDIN_TD_DAILY_TBL_CHECK': 'fill:powderblue',
            'LINKEDIN_ODS_DB_CHECK': 'fill:thistle', 'LI_PIG_JOB' : 'fill:salmon',
            'KFK_SONORA_TD_LOAD' : 'fill:rosybrown', 'KFK_SONORA_HADOOP_GET' : 'fill:khaki',
            'LI_HADOOP_MV' : 'fill:peachpuff', 'LI_ARCHIVE' : 'fill:mistyrose', 'LI_SHELL' : 'fill:lavendar'};

        if (type == 'job')
        {
            for(var i = 0; i < data.nodes.length; i++)
            {
                var path = data.nodes[i].job_path;
                var index = path.lastIndexOf('/');
                var label = path.substring(index+1);
                var style = styles[data.nodes[i].job_type];
                if (!style)
                    style = 'fill:palegoldenrod';
                //g.addNode(i+1, { label: label, style:style, title:path})
                g.setNode(i, { label: label, style:style});
            }
            for(var i = 0; i < data.links.length; i++)
            {
                if (data.links[i].type == 'job')
                {
                    g.setEdge(
                        data.links[i].source,
                        data.links[i].target,
                        {
                            style: "stroke: #66B2FF; stroke-width: 2px; stroke-dasharray: 5, 5;"
                        });
                }
                else
                {
                    g.setEdge(data.links[i].source, data.links[i].target);
                }
            }
        }
        else
        {
            for(var i = 0; i < data.nodes.length; i++)
            {
                var schema_type = '';
                var name;
                var shape = "rect";
                var color;
                if (data.nodes[i].node_type == 'data'){
                    if (data.nodes[i]["storage_type"])
                    {
                        schema_type = data.nodes[i]["storage_type"];
                    }
                    else
                    {
                        schema_type = "hdfs";
                    }

                    if(data.nodes[i].hasOwnProperty("abstracted_path"))
                    {
                        name = data.nodes[i]["abstracted_path"];
                    }
                    else {
                        name = data.nodes[i]['abstracted_object_name'];
                    }
                }
                else if (data.nodes[i].node_type == 'app'){
                    shape = "ellipse";
                    if (data.nodes[i]["name"])
                    {
                        schema_type = data.nodes[i]["name"];
                        name = data.nodes[i]["name"];
                    }
                    else
                    {
                        schema_type = "ETL_job";
                        name = "ETL_job";
                    }
                }
                else if (data.nodes[i].node_type == 'db'){
                    shape = "rect";
                    if (data.nodes[i]["name"])
                    {
                        schema_type = data.nodes[i]["name"];
                        name = data.nodes[i]["name"];
                    }
                    else
                    {
                        schema_type = "Database";
                        name = "Database";
                    }
                }
                else {
                    shape = "ellipse";
                    if (data.nodes[i]["name"])
                    {
                        schema_type = data.nodes[i]["name"];
                        name = data.nodes[i]["name"];
                    }
                    else
                    {
                        schema_type = "default";
                        name = data.nodes[i]["urn"];
                    }
                }

                if (data.nodes[i]["color"]) {
                    color = data.nodes[i]["color"];
                    g.setNode(data.nodes[i].id, { label: name, style:'fill:' + color, id: data.nodes[i].id, shape: shape});
                }
                else {
                    g.setNode(data.nodes[i].id, { label: name, style:'fill:pink', id: data.nodes[i].id, shape: shape});
                }

                /*
                if (schema_type.toLowerCase().includes('druid'))
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:lightblue', id: data.nodes[i].id, shape: shape});
                else if (schema_type.toLowerCase() == 'hdfs')
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:thistle', id: data.nodes[i].id, shape: shape});
                else if (schema_type.toLowerCase().includes('parquet'))
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:tan', id: data.nodes[i].id, shape: shape});
                else if (schema_type.toLowerCase().includes('lucene'))
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:seagreen', id: data.nodes[i].id, shape: shape});
                else if (schema_type.toLowerCase() == 'natezza')
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:mistyrose', id: data.nodes[i].id, shape: shape});
                else if (schema_type.toLowerCase() == 'pulse')
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:sandybrown', id: data.nodes[i].id, shape: shape});
                else if (schema_type.toLowerCase().includes('moveit'))
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:peachpuff', id: data.nodes[i].id, shape: shape});
                else if (schema_type.toLowerCase().includes('qa'))
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:navajowhite', id: data.nodes[i].id, shape: shape});
                else if (schema_type.toLowerCase().includes('sa'))
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:palegreen', id: data.nodes[i].id, shape: shape});
                else if (schema_type.toLowerCase().includes('pim'))
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:orange', id: data.nodes[i].id, shape: shape});
                else if (schema_type.toLowerCase().includes('datamgt'))
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:wheat', id: data.nodes[i].id, shape: shape});
                else if (schema_type.toLowerCase().includes('prospector'))
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:lightcoral', id: data.nodes[i].id, shape: shape});
                else
                    g.setNode(data.nodes[i].id,
                        { label: name, style:'fill:pink', id: data.nodes[i].id, shape: shape});
                */
            }
            for(var i = 0; i < data.links.length; i++)
            {
                var source = data.links[i].source;
                var target = data.links[i].target;
                if (!data.nodes[target].sourceLinks)
                {
                    data.nodes[target].sourceLinks = [];
                }
                data.nodes[target].sourceLinks.push(data.links[i]);
                if (!data.nodes[source].targetLinks)
                {
                    data.nodes[source].targetLinks = [];
                }
                data.nodes[source].targetLinks.push(data.links[i]);
                if (data.links[i].type == 'job')
                {
                    g.setEdge(data.links[i].source, data.links[i].target,
                        {
                            label:data.links[i].label,
                            style: "stroke: #66B2FF; stroke-width: 2px; stroke-dasharray: 5, 5;"
                        });
                }
                else
                {
                    g.setEdge(data.links[i].source, data.links[i].target, {label:data.links[i].label});
                }
            }
        }
        var originalMapScale = 1;

        var styleTooltip = function(name, description) {
            return '<p class="description">' + description + "</p>";
        };

        g.nodes().forEach(function(v) {
            var node = g.node(v);
            // Round the corners of the nodes
            node.rx = node.ry = 5;
        });

        var render = new dagreD3.render();
        var svg = d3.select("#svg-canvas");
        var graphSVG = svg.append("svg").attr("class", "graph-attach").attr("width", "100%").attr("height", "100%");
        var svgGroup = graphSVG.append('g');
        var miniSVG = svg.append("svg").attr("class", "minimap").attr("width", "19.6%").attr("height", "19.5%").attr("x", "80%").attr("y", "0");
        miniSVG.insert('rect', ':first-child')
            .attr('class', 'background')
            .attr('width', '100%')
            .attr('height', '100%')
            .style('fill', '#DDD')
            .style('opacity', '0.5');
        var minimapSVG = miniSVG.append("svg").attr("class", "minimap-attach").attr("width", "100%").attr("height", "100%");
        var overlay = minimapSVG.append('rect', ':first-child')
            .attr('class', 'overlay')
            .attr('width', '100%')
            .attr('height', '100%')
            .style('fill', '#000')
            .style('opacity', '0.1');
        var miniSVGGroup = minimapSVG.append('g');

        graphSVG.node().oncontextmenu = function(d) { return false; };

        var tooltip = d3LineageTooltip();
        tooltip.hide();

        function attachContextMenus() {
            contextMenu.call(graphSVG.node(), graphSVG.selectAll(".node"));
            contextMenu.on("open", function() {
                tooltip.hide();
            }).on("close", function() {

            });
        }

        // Detaches any bound context menus
        function detachContextMenus() {
            $(".graph .node").unbind("contextmenu");
        }

        var origWidth = $('#svg-canvas').width();
        var origHeight = $('#svg-canvas').height();

        var contextMenu = d3LineageContextMenu(svgGroup.node(), svgGroup);
        var minimapScale = 1;

        function selectTabularRow(d, i)
        {
            var str = '#data-table-tr-' + g_currentData.nodes[d].id;
            if (type == 'job')
            {
                str = '#job-data-table-tr-' + g_currentData.nodes[d].id;
            }
            else{
                if (g_currentData.nodes[i].node_type == 'script')
                {
                    str = '#job-table-tr-' + g_currentData.nodes[d].id;
                    if ($("#datatabpage").hasClass('active'))
                    {
                        $("#datatabpage").removeClass('active');
                        $("#jobtabpage").addClass('active');
                        $("#datanodestab").removeClass('active');
                        $("#jobnodestab").addClass('active');
                        $("#nodeInfoSplitter").tabs({ active: 1 });
                    }
                }
                else if (g_currentData.nodes[i].node_type == 'data')
                {
                    if ($("#jobtabpage").hasClass('active'))
                    {
                        $("#jobnodestab").removeClass('active');
                        $("#jobtabpage").removeClass('active');
                        $("#datanodestab").addClass('active');
                        $("#datatabpage").addClass('active');
                        $("#nodeInfoSplitter").tabs({ active: 0 });
                    }
                }
            }

            var obj = $(str);
            $("#nodeInfoSplitter").scrollTo(obj, 800)
            obj.addClass('highlight').siblings().removeClass('highlight');
        }

        var resetViewport = function() {
            var curbbox = svg.node().getBBox();
            var bbox = { x: curbbox.x, y: curbbox.y, width: curbbox.width+50, height: curbbox.height+50};
            var scale = Math.min(origWidth/ (g.graph().width), origHeight/ (g.graph().height));
            if (scale > 1)
                scale = 1;
            originalMapScale = scale;
            minimapScale = scale*0.195;
            var zoomScale = [];
            zoomScale[0] = originalMapScale;
            zoomScale[1] = 1;
            w = origWidth/scale;
            h = origHeight/scale;
            g_scale = scale;
            tx = 0;
            ty = 0;
            zoom
                .translate([0, 0])
                .scale(scale)
                .event(svg);
            svg.attr('height', g.graph().height * scale);
            svg.attr('width', g.graph().width * scale);
            var t = [0, 0];
            miniSVGGroup.attr("transform", "translate(" + t + ")" +
                "scale(" + minimapScale + ")");
        }

        var zoom = d3.behavior.zoom().on("zoom", function() {
            svgGroup.attr("transform", "translate(" + d3.event.translate + ")" +
                "scale(" + d3.event.scale + ")");

            var t = [-d3.event.translate[0]*minimapScale/d3.event.scale, -d3.event.translate[1]*minimapScale/d3.event.scale];

            overlay.attr("x", t[0])
                .attr("y", t[1])
                .attr("width", origWidth/d3.event.scale*minimapScale)
                .attr("height", origHeight/d3.event.scale*minimapScale);
        });

        // Run the renderer. This is what draws the final graph.
        render(svgGroup, g);
        render(miniSVGGroup, g);
        resetViewport();
        svg.call(zoom);
        svgGroup.selectAll(".node")
            .on("click", function(d,i) {
                selectTabularRow(d,i);
            })
            .call(tooltip);
        attachContextMenus();
    }

    $(document).ready(setupSearch);

    if (window.module) {
        module.exports = { init: setupSearch };
    }

}(jQuery));