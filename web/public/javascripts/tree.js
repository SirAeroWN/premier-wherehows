(function (window, $) {
    $(document).ready(function() {

        $("#tree2").fancytree({
            extensions: ["filter"],
            filter: {
                autoApply: true,
                counter: true,
                hideExpandedCounter: true,
                mode: "dimm",
                highlight: true
            },
            source: {
                url: "/tree/datasets"
            }
        });
        /*
        $("#tree1").fancytree({
            source: {
                url: "/tree/metrics"
            }
        });
        */
        $("#tree3").fancytree({
            extensions: ["filter"],
            filter: {
                autoApply: true,
                counter: true,
                hideExpandedCounter: true,
                mode: "dimm",
                highlight: true
            },
            source: {
                url: "/tree/flows"
            },
            lazyLoad: function(event, data){
                var node = data.node;
                var url = '#';
                if (node.data.level == 1)
                {
                    url = "/tree/flow/" + node.title;
                }
                else if (node.data.level == 2)
                {
                    url = "/tree/flow/" + node.data.parent + '/' + node.title;
                }
                data.result = {
                    url: url,
                    cache: false
                };
            }
        });

        $("#tree2").bind("fancytreeactivate", function(event, data){

            var node = data.node;
            if(node)
            {
                if (node.isFolder())
                {
                    if (node.data.level == 1)
                    {
                        window.location = "#/datasets/name/" + node.title + "/page/1?urn=" + node.data.path + ':///';
                    }
                    else
                    {
                        window.location = "#/datasets/name/" + node.title + "/page/1?urn=" + node.data.path + '/';
                    }

                }
                else{
                    if (node && node.data && node.data.id)
                    {
                        window.location = "#/datasets/" + node.data.id;
                    }
                }
            }
        });

        $("#tree2").bind("fancytreeinit", function(event, data){
            if (window.g_currentDatasetNodeName && window.g_currentDatasetNodeUrn)
            {
                findAndActiveDatasetNode(window.g_currentDatasetNodeName, window.g_currentDatasetNodeUrn);
            }
            window.g_currentDatasetNodeName = null;
            window.g_currentDatasetNodeUrn = null;
        });

        $("#tree3").bind("fancytreeactivate", function(event, data){
            var node = data.node;
            if(node)
            {
                if (node.data.level == 1)
                {
                    window.location = "#/flows/name/" + node.title + "/page/1?urn=" + node.title;
                }
                else if(node.data.level == 2)
                {
                    window.location = "#/flows/name/" + node.title +
                        "/page/1?urn=" + node.parent.title + '/' + node.title;
                }
                else if(node.data.level == 3)
                {
                    window.location = "#/flows/name/" + node.parent.parent.title + '/' +
                        node.data.id + '/page/1?urn=' + node.parent.title;
                }
            }
        });


        $("#tree3").bind("fancytreeinit", function(event, data){
            if (window.g_currentFlowApplication)
            {
                findAndActiveFlowNode(window.g_currentFlowApplication,
                    window.g_currentFlowProject,
                    window.g_currentFlowId,
                    window.g_currentFlowName);
            }
            window.g_currentFlowApplication = null;
            window.g_currentFlowProject = null;
            window.g_currentFlowName = null;
            window.g_currentFlowId = null;
        });

        $("#filterinput").val('');
        $("#filterinput").bind("paste keyup", function(){
            var val = $('#filterinput').val();
            var isTreeView = false;
            if ($('#treeviewbtn').hasClass('btn-primary'))
            {
                isTreeView = true;
            }
            if (currentTab == 'Datasets')
            {
                if (isTreeView)
                {
                    $("#tree2").fancytree("getTree").filterNodes(val);
                }
                else
                {
                    filterListView(currentTab, val);
                }
            }
            else
            {
                if (isTreeView)
                {
                    $("#tree3").fancytree("getTree").filterNodes(val);
                }
                else
                {
                    filterListView(currentTab, val);
                }
            }
        });
    });

})(window, jQuery)