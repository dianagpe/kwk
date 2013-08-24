package models;

import play.data.validation.Constraints;
import play.db.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {


    @Constraints.Required
    @Constraints.Email
    public String email;
    public String password;
    public String name;
    public Long id;
    public List<Movie> movies;
    public Double affinity;


    public void lee(){
        Connection connection = null;
        try{
            connection = DB.getConnection();
        }catch(Exception e){

        }finally {
            try{
            if(connection != null) connection.close();
            }catch (Exception e){}
        }
    }

//    public static List<User> calculateSimilarity(){
//
//        Connection connection = null;
//        PreparedStatement stmt = null;
//        ResultSet rs = null;
//
//        List<User> users = new ArrayList<>();
//        User user2 = null;
//        Map<Long, Integer> usersTotalItems = new HashMap<>();
//
//        try{
//            connection = DB.getConnection();
//            stmt = connection.prepareStatement("select p1.usuario_id, count(*) total from pelicula_puntuacion p1, (" +
//                    " select usuario_id from pelicula_puntuacion where pelicula_id in (" +
//                    " select pelicula_id from pelicula_puntuacion where usuario_id = ?) group by usuario_id) p2" +
//                    " where p1.usuario_id = p2.usuario_id group by p1.usuario_id");
//            stmt.setLong(1, user.id);
//            rs = stmt.executeQuery();
//
//            while(rs.next()){
//                usersTotalItems.put(rs.getLong("usuario_id"), rs.getInt("total"));
//            }
//            rs.close();
//
//            stmt = connection.prepareStatement("select p.* from puntuacion p, (" +
//                    "select distinct a.usuario_id usuario_id from puntuacion a, puntuacion b " +
//                    "where a.pelicula_id = b.pelicula_id and b.usuario_id = ? AND a.usuario_id <> ? ) q" +
//                    "where p.usuario_id = q.usuario_id order by p.usuario_id asc, p.puntuacion desc");
//            stmt.setLong(1, user.id);
//            stmt.setLong(2, user.id);
//            rs = stmt.executeQuery();
//
//            while(rs.next()){
//                if(user2 == null || user2.id != rs.getLong("usuario_id")){
//                    users.add( user2 = new User() );
//                }
//              //  user
//
//
//            }
//
//        }catch(Exception e){
//            e.printStackTrace();
//        }finally {
//            try{
//                if (rs != null) rs.close();
//                if (stmt != null) stmt.close();
//                if (connection != null) connection.close();
//            }catch (Exception e){}
//        }
//       // System.out.println(movies.size());
//        //return movies;
//        return null;
//    }

    public static Map<Long, Integer> totalItems2(User user){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        Map<Long, Integer> uti = new HashMap<>();

        try{
            connection = DB.getConnection();
            stmt = connection.prepareStatement("select p1.usuario_id, count(*) total from pelicula_puntuacion p1, (" +
                    " select usuario_id from pelicula_puntuacion where pelicula_id in (" +
                    " select pelicula_id from pelicula_puntuacion where usuario_id = ?) group by usuario_id) p2" +
                    " where p1.usuario_id = p2.usuario_id group by p1.usuario_id");
            stmt.setLong(1, user.id);
            rs = stmt.executeQuery();

            while(rs.next()){
                uti.put(rs.getLong("usuario_id"), rs.getInt("total"));
            }

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            }catch (Exception e){}
        }
        return uti;
    }

    public static Map<Long, User> similarity2(User user){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        Map<Long, Integer> usersti = user.totalItems2(user);
        Map<Long, User> users = new HashMap<>();

        User user1;

        try{

            Integer totalItems = usersti.containsKey(user.id) ? usersti.get(user.id) : 0;
            Integer maxCommonItems;
            Integer commonItems;


            connection = DB.getConnection();
            stmt = connection.prepareStatement("select usuario1_id usuario_id, sim, items_comunes, sum_squares " +
                    "from afinidad where usuario2_id = ? union select usuario2_id usuario_id, sim, items_comunes, sum_squares " +
                    "from afinidad where usuario1_id = ?");
            stmt.setLong(1, user.id);
            stmt.setLong(2, user.id);
            rs = stmt.executeQuery();

            while(rs.next()){

                users.put(rs.getLong("usuario_id"), user1 = new User());
                user1.id = rs.getLong("usuario_id");
                commonItems = rs.getInt("items_comunes");
                maxCommonItems = Math.min(usersti.get(user1.id),totalItems);
                user1.affinity = ((double)commonItems/maxCommonItems) * (1 - Math.tan(Math.sqrt(rs.getDouble("sum_squares")/ commonItems)));
            }

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            }catch (Exception e){}
        }

        return users;
    }

    public static Map<Integer, Integer> totalItems(IdentityUser user){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        Map<Integer, Integer> uti = new HashMap<>();

        try{
            connection = DB.getConnection();
            stmt = connection.prepareStatement("select pa.usuario_id, count(pa.usuario_id) total from pelicula_puntuacion pa" +
                    " left join pelicula_puntuacion pb" +
                    " on pa.pelicula_id = pb.pelicula_id and pb.usuario_id = ? " +
                    " group by pa.usuario_id");
            stmt.setLong(1, user.id);
            rs = stmt.executeQuery();

            while(rs.next()){
                uti.put(rs.getInt("usuario_id"), rs.getInt("total"));
            }

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            }catch (Exception e){}
        }
        return uti;
    }

    public static Map<Integer, IdentityUser> similarity(IdentityUser user){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        Map<Integer, Integer> usersTotalItems = User.totalItems(user);
        Map<Integer, IdentityUser> users = new HashMap<>();

        IdentityUser user1;

        try{

            Integer totalItems = usersTotalItems.containsKey(user.id) ? usersTotalItems.get(user.id) : 0;
            Integer maxCommonItems;
            Integer commonItems;


            connection = DB.getConnection();
            stmt = connection.prepareStatement("select pa.usuario_id, sum(pow(pa.puntuacion - pb.puntuacion,2)) sum_squares, count(*) items_comunes from pelicula_puntuacion pa" +
                    " inner join pelicula_puntuacion pb" +
                    " on pa.pelicula_id = pb.pelicula_id " +
                    " where pb.usuario_id = ? and pa.usuario_id <> ?" +
                    " group by pa.usuario_id");
            stmt.setInt(1, user.id);
            stmt.setInt(2, user.id);
            rs = stmt.executeQuery();

            while(rs.next()){

                users.put(rs.getInt("usuario_id"), user1 = new IdentityUser());
                user1.id = rs.getInt("usuario_id");
                commonItems = rs.getInt("items_comunes");
                maxCommonItems = Math.min(usersTotalItems.get(user1.id),totalItems);

                user1.affinity = ((double)commonItems/maxCommonItems) * (1 - Math.tanh(Math.sqrt(rs.getDouble("sum_squares")/ commonItems)));
                System.out.println("common "+commonItems+" maxcommon ("+totalItems+", "+usersTotalItems.get(user1.id)+") "+
                        Math.min(usersTotalItems.get(user1.id),totalItems)+ " suma cuadrados "+ rs.getDouble("sum_squares")+ "afinidad sin ponderar "+
                        (1 - Math.tanh(Math.sqrt(rs.getDouble("sum_squares")/ commonItems))) + " afinidad ponderada "+ user1.affinity);

            }

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            }catch (Exception e){}
        }

        return users;
    }

    public static User authenticate(String email, String password) {
        System.out.println("entre al authenticate");

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        User user = null;

        try{

            connection = DB.getConnection();
            stmt = connection.prepareStatement("select u.usuario_id, u.nombre from usuario u where email = ? and password = ?");
            stmt.setString(1, email);
            stmt.setString(2, password);
            rs = stmt.executeQuery();

            if(rs.next()){
                user = new User();
                user.id = rs.getLong("usuario_id");
                user.name = rs.getString("nombre");
                user.email = email;
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

    public String validate() {
        if(User.authenticate(email, password) == null) {
            return "Invalid user or password";
        }
        return null;
    }

    /*public static User getUser(String email, String password){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        Map<Long, Integer> uti = new HashMap<>();

        try{
            connection = DB.getConnection();
            stmt = connection.prepareStatement("select p1.usuario_id, count(*) total from pelicula_puntuacion p1, (" +
                    " select usuario_id from pelicula_puntuacion where pelicula_id in (" +
                    " select pelicula_id from pelicula_puntuacion where usuario_id = ?) group by usuario_id) p2" +
                    " where p1.usuario_id = p2.usuario_id group by p1.usuario_id");
           // stmt.setLong(1, user.id);
            rs = stmt.executeQuery();

            while(rs.next()){
                uti.put(rs.getLong("usuario_id"), rs.getInt("total"));
            }

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            }catch (Exception e){}
        }
        return uti;
    }               */

}
