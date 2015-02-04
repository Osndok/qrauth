
"use strict";

/*
 * widget.js
 */

function startPageSync()
{
	if (window.XMLHttpRequest)
	{
		syncQuery = new XMLHttpRequest();
	}
	else
	{
		syncQuery = new ActiveXObject('MSXML2.XMLHTTP.3.0');
	}

	syncQuery.open( 'GET', 'sync.htm' );

	syncQuery.onreadystatechange = function()
	{
		if ( syncQuery.readyState == 4 )
			document.location.href = 'demo.htm';
	}

	syncQuery.send();
}