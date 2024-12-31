package antifraud.DTO;

import antifraud.constants.Role;

public class UserDTO {

    private Long id;
    private String name;
    private Role role;
    private String username;

    public UserDTO(Long id, String name, Role role, String username) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", role='" + role.toString() + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
