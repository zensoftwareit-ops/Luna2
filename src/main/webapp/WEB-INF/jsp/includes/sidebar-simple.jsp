<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<style>
    .sidebar {
        position: fixed;
        top: 0;
        left: 0;
        height: 100vh;
        width: 250px;
        background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
        overflow-y: auto;
        z-index: 1000;
        padding: 20px;
    }
    .sidebar .nav-link {
        color: rgba(255,255,255,0.8);
        padding: 12px 20px;
        border-radius: 8px;
        margin: 4px 0;
        transition: all 0.3s;
        text-decoration: none;
        display: block;
    }
    .sidebar .nav-link:hover {
        background: rgba(255,255,255,0.2);
        color: white;
    }
    .content-wrapper {
        margin-left: 250px;
        width: calc(100% - 250px);
        background-color: #f8f9fa;
        min-height: 100vh;
    }
    @media (max-width: 991.98px) {
        .sidebar {
            position: static;
            width: 100%;
            height: auto;
        }
        .content-wrapper {
            margin-left: 0;
            width: 100%;
        }
    }
</style>
<div class="sidebar">
    <div style="text-align: center; margin-bottom: 30px;">
        <h5 style="color: white; font-weight: bold;">Luna2</h5>
    </div>
    <ul style="list-style: none; padding: 0;">
        <li><a class="nav-link" href="/luna2/app/dashboard.action">Dashboard</a></li>
        <li><a class="nav-link" href="/luna2/app/magazzino/list">Magazzino</a></li>
        <li><a class="nav-link" href="/luna2/logout.action">Logout</a></li>
    </ul>
</div>
