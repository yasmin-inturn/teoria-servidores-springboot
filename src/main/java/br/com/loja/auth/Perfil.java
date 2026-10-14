package br.com.loja.auth;

/**
 * Perfis de acesso. O nome vai para o claim "roles" do JWT e o Spring Security
 * o transforma em ROLE_CLIENTE / ROLE_ADMIN, formato esperado por hasRole().
 */
public enum Perfil {

    CLIENTE,
    ADMIN
}
