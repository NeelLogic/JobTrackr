package solannee.sheridancollege.ca.jobtrackr.dto.auth;
public record AuthResponse(String token,String tokenType,long expiresIn,UserResponse user) {}
