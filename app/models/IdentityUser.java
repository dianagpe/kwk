package models;


import play.db.DB;
import scala.Option;
import securesocial.core.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;

public class IdentityUser implements Identity {

    public Integer id;
    public IdentityId identityId;
    public String firstName;
    public String lastName;
    public String fullName;
    public Option<String> email;
    public Option<String> avatarUrl;
    public AuthenticationMethod authMethod;
    public Option<OAuth1Info> oAuth1Info;
    public Option<OAuth2Info> oAuth2Info;
    public Option<PasswordInfo> passwordInfo;

    public List<Movie> movies;
    public Double affinity;

    @Override
    public IdentityId identityId() {
        return this.identityId;
    }

    @Override
    public String firstName() {
        return this.firstName;
    }

    @Override
    public String lastName() {
        return this.lastName;
    }

    @Override
    public String fullName() {
        return this.fullName;
    }

    @Override
    public Option<String> email() {
        return this.email;
    }

    @Override
    public Option<String> avatarUrl() {
        return this.avatarUrl;
    }

    @Override
    public AuthenticationMethod authMethod() {
        return this.authMethod;
    }

    @Override
    public Option<OAuth1Info> oAuth1Info() {
        return this.oAuth1Info;
    }

    @Override
    public Option<OAuth2Info> oAuth2Info() {
        return this.oAuth2Info;
    }

    @Override
    public Option<PasswordInfo> passwordInfo() {
        return this.passwordInfo;
    }

    public static IdentityUser save(Identity identity){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        String[] keys = {"usuario_id"};
        Integer userId = find(identity);
        IdentityUser user = null;

        try{
            connection = DB.getConnection();
            stmt = connection.prepareStatement("insert into usuario (usuario_id, email, nombre, ultimo_acceso, password, oauth_proveedor, oauth_usuario)" +
                    " values (?, ?, ?, CURRENT_TIMESTAMP, null, ?, ?)" +
                    "  ON DUPLICATE KEY UPDATE ultimo_acceso = CURRENT_TIMESTAMP", keys);

            if(userId!=null)
                stmt.setInt(1, userId);
            else {
                stmt.setNull(1, Types.INTEGER);
            }
            if(identity.email() != null)
                stmt.setString(2, identity.email().toString());
            else
                stmt.setNull(2, Types.VARCHAR);

            stmt.setString(3, identity.fullName());
            stmt.setString(4, identity.identityId().providerId());
            stmt.setString(5, identity.identityId().userId());
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();

            if(rs.next()){
                user = new IdentityUser();
                user.identityId = identity.identityId();
                user.email = identity.email();
                user.fullName = identity.fullName();
                user.firstName = identity.firstName();
                user.lastName = identity.lastName();
                user.id = rs.getInt(1);
            }

        } catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            }catch (Exception e){}
        }
        return user;

    }

    public static Integer find(Identity identity) {

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        Integer userId = null;

        try{
            connection = DB.getConnection();
            stmt = connection.prepareStatement("select usuario_id from usuario where oauth_proveedor = ? and oauth_usuario = ?");
            stmt.setString(1, identity.identityId().providerId());
            stmt.setString(2, identity.identityId().userId());
            rs = stmt.executeQuery();

            if(rs.next()){
                userId = rs.getInt("usuario_id");
            }


        } catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            }catch (Exception e){}
        }
        return userId;
    }
}
