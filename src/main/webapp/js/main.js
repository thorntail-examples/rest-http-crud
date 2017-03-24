// The root URL for the RESTful services
var rootURL = "http://"+window.location.hostname+":"+window.location.port+"/api/fruits";

var currentData;

// Retrieve data list when application starts
findAll();

// Nothing to delete in initial application state
$('#btnDelete').hide();

// Register listeners
$('#btnSearch').click(function() {
	search($('#searchKey').val());
	return false;
});

$('#btnAdd').click(function() {
	newData();
	return false;
});

$('#btnSave').click(function() {
	if ($('#dataId').val() == '')
		addData();
	else
		updateData();
	return false;
});

$('#btnDelete').click(function() {
	deleteData();
	return false;
});

$('#dataList a').live('click', function() {
	findById($(this).data('identity'));
});

function search(searchKey) {
	if (searchKey == '')
		findAll();
	else
		findByName(searchKey);
}

function newData() {
	$('#btnDelete').hide();
	clearData();
}

function clearData() {
	currentData = {};
	renderDetails(currentData); // Display empty form
}

function findAll() {
	clearData();
	console.log('findAll');
	$.ajax({
		type: 'GET',
		url: rootURL,
		dataType: "json", // data type of response
		success: renderList
	});
}

function findByName(searchKey) {
	clearData();
	console.log('findByName: ' + searchKey);
	$.ajax({
		type: 'GET',
		url: rootURL + '/search/' + searchKey,
		dataType: "json",
		success: renderList
	});
}

function findById(id) {
	console.log('findById: ' + id);
	$.ajax({
		type: 'GET',
		url: rootURL + '/' + id,
		dataType: "json",
		success: function(data){
			$('#btnDelete').show();
			console.log('findById success: ' + data.name);
			currentData = data;
			renderDetails(currentData);
		}
	});
}

function addData() {
	console.log('addData');
	$.ajax({
		type: 'POST',
		contentType: 'application/json',
		url: rootURL,
		dataType: "json",
		data: formToJSON(),
		success: function(data, textStatus, jqXHR){
			alert('Data created successfully');
			$('#btnDelete').show();
			findAll();
		},
		error: function(jqXHR, textStatus, errorThrown){
			alert('addData  error: ' + textStatus);
		}
	});
}

function updateData() {
	alert('Updating data is not supported');
	/*console.log('updateData');
	$.ajax({
		type: 'PUT',
		contentType: 'application/json',
		url: rootURL + '/' + $('#dataId').val(),
		dataType: "json",
		data: formToJSON(),
		success: function(data, textStatus, jqXHR){
			alert('Data updated successfully');
		},
		error: function(jqXHR, textStatus, errorThrown){
			alert('updateData error: ' + textStatus);
		}
	});*/
}

function deleteData() {
	console.log('deleteData');
	$.ajax({
		type: 'DELETE',
		url: rootURL + '/' + $('#dataId').val(),
		success: function(data, textStatus, jqXHR){
			alert('Data deleted successfully');
			findAll();
		},
		error: function(jqXHR, textStatus, errorThrown){
			alert('deleteData error');
		}
	});
}

function renderList(data) {
	// JAX-RS serializes an empty list as null, and a 'collection of one' as an object (not an 'array of one')
	var list = data == null ? [] : (data instanceof Array ? data : [data]);

	$('#dataList li').remove();
	$.each(list, function(index, data) {
		$('#dataList').append('<li><a href="#" data-identity="' + data.id + '">'+data.name+'</a></li>');
	});
}

function renderDetails(data) {
	$('#dataId').val(data.id);
	$('#name').val(data.name);
}

// Helper function to serialize all the form fields into a JSON string
function formToJSON() {
	var dataId = $('#dataId').val();
	return JSON.stringify({
		"id": dataId == "" ? null : dataId,
		"name": $('#name').val()
		});
}
