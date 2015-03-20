<?php

ob_start();

?><!DOCTYPE html>
<html>
<head>
	<title>qrauth: thin tenant simulator and POC</title>
</head>
<body>

<!--

The purpose of this file is to illustrate how the qrauth service might be
used from a PHP application.

UNLIKE a real application, this one:
(1) will probably expose it's secret API-KEY (!!!),
(2) will show intermediate steps that a production site would surly hide, and
(3) has no "real" page to forward the user to (no app to protect)

-->

<pre>
SCRIPT_NAME=<?=($script_name=$_SERVER['SCRIPT_NAME'])?>

REQUEST_URI=<?=($request_uri=$_SERVER['REQUEST_URI'])?>

config=<?=($config=basename($request_uri))?>


</pre>

<?php

function e($number=500, $message='Internal Error')
{
	headers_sent() or header('x', true, $number);
	error_log('e'.$number.': '.$message);
	echo "<br/><br/><hr/>\nERROR: ";
	die($message);
}

strpos($config, '.ini')       or e(400, "missing or invalid configuration parameter");
strpos($config, '..'  )      and e(400, "bad configuration parameter; ..");
strpos($config, '/'   )      and e(400, "bad configuration parameter; /");
$ini=parse_ini_file($config)  or e(400, "did not find config file: $config");

if (false)
{
	echo "<pre>\nConfiguration ";
	var_export($ini);
	echo ";\n</pre>\n";
}

$endpoint=$ini['endpoint']    or e(500, "$config: missing 'endpoint'");
$api_key =$ini['api_key' ]    or e(500, "$config: missing 'api_key'" );

session_start();

$has_deadline=isset($_SESSION['deadline']);

if ($has_deadline and stillInTheFuture($_SESSION['deadline']))
{
	echo "<pre>You are logged in.\n\nLogin information (from session) ";
	var_export($_SESSION);
	echo ";\n</pre>\n";
	echo "\nTODO: implement logout button";
}
else
{
	define('BASEPATH', dirname($script_name));
	require 'qrauth_helper.php';

	$data=array();
	$data['api_key']=$api_key;
	$data['user_ip']=$_SERVER['REMOTE_ADDR'];
	$data['session_id']=sha1(session_id());

	$response=qrauth_post($data, '/api/tenant/login', $endpoint);

	echo "\nqrauth response ";
	var_export($response);
	echo ";\nerror?=$qrauth_error\nstatus=$qrauth_status\n";

	if ($qrauth_status==409)
	{
		echo "<br/><br/>\n\n";
		echo "In production, we would retry with a new session id or cause a page refresh here. ";
		echo "For now, just hit refresh yourself!";
		//setcookie (session_name(), "", time() - 3600);
		session_destroy();
		session_start();
		session_regenerate_id();
	}
	else
	if ($qrauth_status==200)
	{
		session_destroy();
		session_start();

		foreach(json_decode($response) as $key=>$value)
		{
			$_SESSION[$key]=$value;
		}

		session_set_cookie_params($_SESSION['seconds']);
	}
}


function stillInTheFuture($millisDate)
{
	$deadline=$millisDate/1000;
	$now=time();
	$left=floor($deadline-$now);
	echo "<pre>\n";
	echo "deadline=$deadline\n";
	echo "now_secs=$now\n";
	echo "timeleft=$left seconds\n";
	echo "</pre>\n";
	return $deadline > $now;
}

?>


</body>
</html>

<?php

ob_end_flush();


