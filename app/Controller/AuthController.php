<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;

final class AuthController extends BaseController
{
    public function form(): never
    {
        if (Auth::check()) {
            $this->redirect('/dashboard');
        }
        $this->view->render('auth/login', ['title' => 'Accedi', 'layout' => false]);
    }

    public function login(): never
    {
        $email = mb_strtolower(trim((string) ($_POST['email'] ?? '')));
        $password = (string) ($_POST['password'] ?? '');
        $ip = substr((string) ($_SERVER['REMOTE_ADDR'] ?? ''), 0, 45);
        $limit = $this->db->prepare("SELECT COUNT(*) FROM login_attempts WHERE email = ? AND ip_address = ? AND successful = 0 AND created_at >= DATE_SUB(NOW(), INTERVAL 15 MINUTE)");
        $limit->execute([$email, $ip]);
        if ((int) $limit->fetchColumn() >= 5) {
            $_SESSION['login_error'] = 'Troppi tentativi. Riprova tra 15 minuti.';
            $_SESSION['old_email'] = $email;
            $this->redirect('/login');
        }
        $auth = new Auth($this->db);
        if (!$auth->attempt($email, $password)) {
            $this->db->prepare('INSERT INTO login_attempts (email, ip_address, successful) VALUES (?, ?, 0)')->execute([$email, $ip]);
            $_SESSION['login_error'] = 'Credenziali non valide.';
            $_SESSION['old_email'] = $email;
            $this->redirect('/login');
        }
        $this->db->prepare('INSERT INTO login_attempts (email, ip_address, successful) VALUES (?, ?, 1)')->execute([$email, $ip]);
        $this->db->prepare('DELETE FROM login_attempts WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)')->execute();
        $this->audit('LOGIN', 'user', Auth::id());
        $this->redirect('/dashboard');
    }

    public function logout(): never
    {
        $userId = Auth::id();
        $this->audit('LOGOUT', 'user', $userId);
        Auth::logout();
        header('Location: /login');
        exit;
    }
}
