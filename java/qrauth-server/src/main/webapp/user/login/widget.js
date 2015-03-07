
"use strict";

/*
 * widget.js
 */

function qrauth_sqrlClick()
{
	var syncQuery;

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
		{
			document.location.href = 'demo.htm';
		}
	}

	syncQuery.send();
}

function qrauth_ppp_challenge()
{
	var syncQuery;

	if (window.XMLHttpRequest)
	{
		syncQuery = new XMLHttpRequest();
	}
	else
	{
		syncQuery = new ActiveXObject('MSXML2.XMLHTTP.3.0');
	}

	var element=document.getElementById('ppp_username');
	var url=element.getAttribute('url');
	var username=element.value;

	syncQuery.open('GET', url+'/'+username);
	syncQuery.onreadystatechange = function() {
		if ( syncQuery.readyState == 4 )
		{
			document.getElementById('ppp_challenge').innerHTML=syncQuery.response;
			document.getElementById('ppp_response').focus();
		}
	}
	syncQuery.send();
	return false;
}

function qrauth_ppp_keyup(event)
{
	if ((event.which || event.keyCode)==13)
	{
		qrauth_ppp_challenge();
		event.preventDefault();
		return false;
	}

	return true;
}

function qrauth_submit_check()
{
	return true;
}

tabby.init();

new ZeroClipboard(document.getElementById('qrauth_clipboard1'));
