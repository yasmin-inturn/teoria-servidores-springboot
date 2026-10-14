package br.com.loja.auth;

import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Usuario da loja. A senha nunca e guardada em texto puro: o campo abaixo
 * recebe o hash BCrypt gerado no cadastro (ver AuthService).
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(nullable = false, length = 120)
    private String nome;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_perfil", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 20)
    private Set<Perfil> perfis = EnumSet.noneOf(Perfil.class);

    @Column(nullable = false)
    private boolean ativo = true;

    protected Usuario() {
        // exigido pelo JPA
    }

    public Usuario(String email, String senhaHash, String nome, Set<Perfil> perfis) {
        this.email = email.toLowerCase();
        this.senhaHash = senhaHash;
        this.nome = nome;
        this.perfis = EnumSet.copyOf(perfis);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getNome() {
        return nome;
    }

    public Set<Perfil> getPerfis() {
        return Set.copyOf(perfis);
    }

    public boolean isAtivo() {
        return ativo;
    }
}
