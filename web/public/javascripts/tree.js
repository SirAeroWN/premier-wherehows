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
        });
    });

})(window, jQuery)