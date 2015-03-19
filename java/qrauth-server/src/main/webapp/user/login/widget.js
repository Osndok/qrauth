
"use strict";

/*
 * widget.js
 */

function qrauth_sqrl_click()
{
	//This indicates the sqrl code has been clicked, so we might as well obscure the code (to hide the NUT value).
	//It is unclear if this provides any real security advantage, but might help in the event of a DoS or practical joke?

	var qrCode=document.getElementById('qrauth_sqrl_qr_nut');

	qrCode.setAttribute('src', qrCode.getAttribute('pending'));

	return true;
}

function qrauth_sms_click()
{
	var e=document.getElementById('qrauth_sms_hidden');

	if (e.getAttribute('style'))
	{
		e.removeAttribute('style');
		//TODO: send the phone number via XMLHttpRequest
		return false;
	}
	else
	{
		//Second click falls through to submit the form...
		return true;
	}
}

var qrauth_nut_state="INIT";

function qrauth_nut_state_change(newState)
{
	if (newState=="LIMBO")
	{
		var qrCode=document.getElementById('qrauth_sqrl_qr_nut');
    	qrCode.setAttribute('src', qrCode.getAttribute('pending'));
    	return false;
	}
	else
	if (newState=="READY")
	{
		//TODO: test various browsers to determine if any common ones will forbid clicking a submit button inside a noscript tag
		//document.getElementById('qrauth_do_sqrl').click();
		document.qrauth_form.do_sqrl.click();
		return true;
	}
	else
	{
		var qrCode=document.getElementById('qrauth_sqrl_qr_nut');
    	qrCode.setAttribute('src', qrCode.getAttribute('failure'));
    	return true;
	}
}

var qrauth_polling_period=1500;

var qrauth_origination_time=Date.now();

function qrauth_poll_nut()
{
	var qrCode=document.getElementById('qrauth_sqrl_qr_nut');
	var url=qrCode.getAttribute('poll');

	var syncQuery;

	if (window.XMLHttpRequest)
	{
		syncQuery = new XMLHttpRequest();
	}
	else
	{
		syncQuery = new ActiveXObject('MSXML2.XMLHTTP.3.0');
	}

	syncQuery.open( 'GET', url );

	var requestStartTime=new Date().getTime();

	syncQuery.onreadystatechange = function()
	{
		if ( syncQuery.readyState == 4 )
		{
			var now=Date.now();
			var requestDuration=now-requestStartTime;
			var timeOnPage=now-qrauth_origination_time;

			if (syncQuery.status==200)
			{
				var newState=syncQuery.response;

				if (newState!=qrauth_nut_state)
				{
					console.log("nut state change: "+qrauth_nut_state+" -> "+newState);

					if (qrauth_nut_state_change(newState))
					{
						return;
					}

					qrauth_nut_state=newState;
				}
			}

			//At some point... we should probably "give up" or a speedy response and just show the "I have sent the SQRL" button again.
			//1-minute  (debugging): if (timeOnPage>60000)
			//1-day    (production):
            if (timeOnPage>86400000)
			{
				qrauth_page_is_stale();
			}
			else
			if (qrauth_polling_period<7000)
			{
				qrauth_polling_period=Math.floor(qrauth_polling_period*1.01);
				setTimeout(qrauth_poll_nut, qrauth_polling_period+3*requestDuration);
			}
			else
			{
				setTimeout(qrauth_poll_nut, qrauth_polling_period+3*requestDuration);
			}
		}
	};

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

function qrauth_page_is_stale()
{
	document.qrauth_form.do_sqrl.removeAttribute("style");
	document.getElementById('qrauth_sms_stale').setAttribute('style', 'display:block');
	document.getElementById('qrauth_sms_fresh').setAttribute('style', 'display:none');
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

{
	var clip1=document.getElementById('qrauth_clipboard1');

	if (clip1)
	{
		new ZeroClipboard(clip1);

		//TODO: technically unrelated to this test, but presence of clippable element is a decent test for login form/standalone-sqrl
		tabby.init();
	}
}

/*
 * It is reasonable to wait anywhere from 6 to 12 seconds before beginning to poll the nut.
 * 6 seconds is the measured "power user" with the default sqrl client at standby (short password, quick decode).
 */
setTimeout(qrauth_poll_nut, 6000);

/*
Since the user obviously has javascript enabled, we can hide the "do_sqrl" submit button, which we would not
be able to access if we were to hide in a noscript tag (as usual "no javascript compatibility" widgets go).
*/
document.addEventListener('DOMContentLoaded', function() {
	document.qrauth_form.do_sqrl.setAttribute("style", "display:none;");
});
