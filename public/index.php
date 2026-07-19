<?php

declare(strict_types=1);

use Luna\Core\Application;

define('LUNA_START', microtime(true));
define('BASE_PATH', dirname(__DIR__));

require BASE_PATH . '/vendor/autoload.php';

$app = Application::boot(BASE_PATH);
$app->run();
