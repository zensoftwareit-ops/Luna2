<?php

declare(strict_types=1);

namespace Luna\Core;

use PDO;
use PDOException;
use RuntimeException;

final class Database
{
    public static function connect(): PDO
    {
        $host = (string) Env::get('DB_HOST', 'localhost');
        $port = Env::int('DB_PORT', 3306);
        $database = (string) Env::get('DB_DATABASE', 'luna2');
        $username = (string) Env::get('DB_USERNAME', 'luna2_user');
        $password = (string) Env::get('DB_PASSWORD', '');
        $dsn = "mysql:host={$host};port={$port};dbname={$database};charset=utf8mb4";

        try {
            return new PDO($dsn, $username, $password, [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
                PDO::ATTR_STRINGIFY_FETCHES => false,
            ]);
        } catch (PDOException $exception) {
            throw new RuntimeException('Connessione al database non disponibile.', 0, $exception);
        }
    }
}
