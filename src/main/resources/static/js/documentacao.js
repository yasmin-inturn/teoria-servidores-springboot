/*
 * JS da pagina de documentacao: so conforto de leitura (tema e copiar codigo).
 * Nenhuma regra de negocio no navegador - quem decide tudo e o servidor.
 */
(function () {
    'use strict';

    // ---- tema claro/escuro -------------------------------------------------
    var raiz = document.documentElement;
    var salvo = null;
    try {
        salvo = localStorage.getItem('tema-doc');
    } catch (e) {
        salvo = null; // navegacao privada pode bloquear o localStorage
    }
    if (salvo === 'claro' || salvo === 'escuro') {
        raiz.setAttribute('data-tema', salvo);
    }

    var botaoTema = document.getElementById('alternarTema');
    if (botaoTema) {
        botaoTema.addEventListener('click', function () {
            var escuroAgora = raiz.getAttribute('data-tema') === 'escuro'
                || (!raiz.hasAttribute('data-tema')
                    && window.matchMedia('(prefers-color-scheme: dark)').matches);
            var novo = escuroAgora ? 'claro' : 'escuro';
            raiz.setAttribute('data-tema', novo);
            try {
                localStorage.setItem('tema-doc', novo);
            } catch (e) {
                /* sem persistencia, segue funcionando na sessao */
            }
        });
    }

    // ---- botoes "Copiar" ---------------------------------------------------
    document.querySelectorAll('[data-copiar]').forEach(function (botao) {
        botao.addEventListener('click', function () {
            var caixa = botao.closest('.arquivo, .comando');
            if (!caixa) {
                return;
            }
            var alvo = caixa.querySelector('pre code') || caixa.querySelector('code');
            if (!alvo) {
                return;
            }
            var texto = alvo.textContent;
            var rotulo = botao.textContent;

            var avisar = function (mensagem) {
                botao.textContent = mensagem;
                setTimeout(function () {
                    botao.textContent = rotulo;
                }, 1400);
            };

            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(texto).then(function () {
                    avisar('Copiado');
                }, function () {
                    avisar('Falhou');
                });
            } else {
                avisar('Sem suporte');
            }
        });
    });
})();
