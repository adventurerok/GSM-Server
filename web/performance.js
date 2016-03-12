/**
 * Created by paul on 12/03/16.
 */

var chart;
var chartData = [];
var chartCursor;
var second = 0;
var firstDate = new Date();

// generate some random data, quite different range
function generateChartData() {
    for (day = 0; day < 50; day++) {
        var newDate = new Date(firstDate);
        newDate.setDate(newDate.getDate() + day);

        var visits = Math.round(Math.random() * 40) - 20;

        chartData.push({
            date: newDate,
            visits: visits
        });
    }
}

// create chart
AmCharts.ready(function() {

    // SERIAL CHART
    chart = new AmCharts.AmSerialChart();
    chart.pathToImages = "http://www.amcharts.com/lib/images/";
    chart.marginTop = 0;
    chart.marginRight = 10;
    chart.autoMarginOffset = 5;
    chart.zoomOutButton = {
        backgroundColor: '#000000',
        backgroundAlpha: 0.15
    };
    chart.dataProvider = chartData;
    chart.categoryField = "date";

    // AXES
    // category
    var categoryAxis = chart.categoryAxis;
    categoryAxis.parseDates = true; // as our data is date-based, we set parseDates to true
    categoryAxis.minPeriod = "ss"; // our data is daily, so we set minPeriod to DD
    categoryAxis.dashLength = 1;
    categoryAxis.gridAlpha = 0.15;
    categoryAxis.axisColor = "#DADADA";

    // value
    var valueAxis = new AmCharts.ValueAxis();
    valueAxis.axisAlpha = 0.2;
    valueAxis.dashLength = 1;
    chart.addValueAxis(valueAxis);

    // GRAPH
    var graph = new AmCharts.AmGraph();
    graph.title = "red line";
    graph.valueField = "visits";
    graph.lineColorField = "lineColor";
    graph.fillColorsField = "lineColor";
    graph.bullet = "round";
    graph.bulletBorderColor = "#FFFFFF";
    graph.bulletBorderThickness = 2;
    graph.lineThickness = 2;
    graph.hideBulletsCount = 50; // this makes the chart to hide bullets when there are more than 50 series in selection
    chart.addGraph(graph);

    // CURSOR
    chartCursor = new AmCharts.ChartCursor();
    chartCursor.cursorPosition = "mouse";
    chart.addChartCursor(chartCursor);

    // SCROLLBAR
    var chartScrollbar = new AmCharts.ChartScrollbar();
    chartScrollbar.graph = graph;
    chartScrollbar.scrollbarHeight = 40;
    chartScrollbar.color = "#FFFFFF";
    chartScrollbar.autoGridCount = true;
    chart.addChartScrollbar(chartScrollbar);

    // WRITE
    chart.write("chartdiv");

    // set up the chart to update every second
    setInterval(function () {
        // normally you would load new datapoints here,
        // but we will just generate some random values
        // and remove the value from the beginning so that
        // we get nice sliding graph feeling

        // remove datapoint from the beginning
        if(chartData.length >= 50) {
            chart.dataProvider.shift();
        }

        // add new one at the end
        second++;
        var newDate = new Date(firstDate);
        newDate.setSeconds(newDate.getSeconds() + second);

        var xhr = new XMLHttpRequest();

        xhr.open("GET", "/api/client/resource_usage?client=cwtest", false);
        xhr.send();

        var json = JSON.parse(xhr.responseText);

        var visits = json["performance"];


        chart.dataProvider.push({
            date: newDate,
            visits: visits,
            lineColor: "#00ffff"
        });
        chart.validateData();
    }, 1000);
});



