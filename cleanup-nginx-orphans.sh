#!/bin/bash
# Cleanup symlink rotti nginx host dopo eliminazione istanze

echo "==================================================="
echo "Cleanup Nginx Orphaned Symlinks"
echo "==================================================="
echo ""

CLEANED=0

# Controlla /etc/nginx/conf.d/
if [ -d "/etc/nginx/conf.d" ]; then
    echo "→ Controllando /etc/nginx/conf.d/ ..."
    for link in /etc/nginx/conf.d/*.conf; do
        [ -e "$link" ] && continue
        [ -L "$link" ] || continue
        echo "  ✗ Rimosso symlink rotto: $link"
        rm -f "$link"
        ((CLEANED++))
    done
fi

# Controlla /etc/nginx/sites-enabled/
if [ -d "/etc/nginx/sites-enabled" ]; then
    echo "→ Controllando /etc/nginx/sites-enabled/ ..."
    for link in /etc/nginx/sites-enabled/*; do
        [ -e "$link" ] && continue
        [ -L "$link" ] || continue
        echo "  ✗ Rimosso symlink rotto: $link"
        rm -f "$link"
        ((CLEANED++))
    done
fi

echo ""
if [ "$CLEANED" -gt 0 ]; then
    echo "✓ Rimossi $CLEANED symlink rotti"
    echo ""
    echo "→ Test configurazione nginx..."
    if nginx -t; then
        echo "✓ Configurazione nginx valida"
        echo ""
        echo "→ Reload nginx..."
        systemctl reload nginx && echo "✓ Nginx reloaded" || echo "✗ Reload fallito"
    else
        echo "✗ Configurazione nginx ancora non valida"
        echo "   Controlla manualmente: nginx -T | grep 'conf.d\\|sites-enabled'"
    fi
else
    echo "✓ Nessun symlink rotto trovato"
fi

echo ""
echo "==================================================="
