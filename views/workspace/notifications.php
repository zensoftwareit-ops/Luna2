<?php use Luna\Core\Csrf; use Luna\Core\View; $token = View::e(Csrf::token()); ?>
<section class="page-intro compact">
    <div><span class="eyebrow">Workspace</span><h1>Centro notifiche</h1><p>Scadenze, anomalie e attività che richiedono attenzione.</p></div>
    <?php if ($notifications): ?><form method="post" action="/workspace/notifications/read-all"><input type="hidden" name="_token" value="<?= $token ?>"><button class="button ghost" type="submit"><?= View::icon('check') ?> Segna tutte come lette</button></form><?php endif; ?>
</section>
<section class="notification-page-list">
    <?php foreach ($notifications as $notification): ?>
        <article class="notification-card severity-<?= View::e(strtolower((string) $notification['severity'])) ?><?= empty($notification['read_at']) ? ' unread' : '' ?>">
            <span class="notification-status"><?= View::icon($notification['severity'] === 'DANGER' || $notification['severity'] === 'WARNING' ? 'alert' : 'check') ?></span>
            <div>
                <span class="section-kicker"><?= View::e($notification['category']) ?> · <?= View::date($notification['created_at']) ?></span>
                <h2><?= View::e($notification['title']) ?></h2>
                <p><?= View::e($notification['message']) ?></p>
            </div>
            <div class="notification-actions">
                <?php if (!empty($notification['action_url'])): ?><a class="button compact-button" href="<?= View::e($notification['action_url']) ?>">Apri</a><?php endif; ?>
                <?php if (empty($notification['read_at'])): ?>
                    <form method="post" action="/workspace/notifications/<?= (int) $notification['id'] ?>/read">
                        <input type="hidden" name="_token" value="<?= $token ?>"><button class="table-action" type="submit">Segna letta</button>
                    </form>
                <?php endif; ?>
            </div>
        </article>
    <?php endforeach; ?>
    <?php if (!$notifications): ?><section class="card empty-state-large"><?= View::icon('check') ?><h2>Tutto sotto controllo</h2><p>Non ci sono notifiche attive per questo workspace.</p></section><?php endif; ?>
</section>
