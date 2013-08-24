package models;


import play.db.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class Movie {

    public Integer id;
    public String name;
    public String nameEs;
    public String cast;
    public Integer year;
    public Double rating = 0.0;
    public String country;
    public String director;
    public String img ;//=  "http://lorempixel.com/160/230/people/";
    public Date releaseDate;

    public static enum Set{
        ALL("all"), SEEN("seen"), UNSEEN("unseen");

        public String id;

        private Set(String id){
            this.id = id;
        }

        public static Set getById(String id){
            for(Set set : values()){
                if(set.id.equals(id))
                    return set;
            }
            return Set.UNSEEN;
        }

    }


    public Movie (){
    }

    public Movie(Integer id, String name, Integer year){
        this.id = id;
        this.name = name;
        this.year = year;
    }

    public static List<Movie> list(Movie.Set set, Integer userId, Integer offset, Integer limit, String search, Integer orderBy){

        String query, condition, join, match;

        switch (set){
            case SEEN:
                condition = "";
                join = "inner";
                break;
            case UNSEEN:
                condition = "where pp.pelicula_id is null";
                join = "left";
                break;
            default:
                condition = "";
                join = "left";
        }

        match = (search != null && !search.isEmpty())
                ? ((set == Set.UNSEEN) ?" and " : " where ") + " match (nombre_es, nombre_en, director, pais, reparto) against (? IN BOOLEAN MODE) "
                : "";

//        String extra = "";
//        if(search == null || search.isEmpty()){
//            extra =  ((set == Set.UNSEEN) ?" and " : " where ") + " anio = 2011 AND pais in ('Estados Unidos','México') ";
//        }

        query = "SELECT p.pelicula_id, p.guion, p.director, p.nombre_en, p.nombre_es, p.anio, p.pais, p.reparto, pp.puntuacion" +
                " from pelicula p " + join +
                " join pelicula_puntuacion pp" +
                " on pp.pelicula_id = p.pelicula_id and pp.usuario_id = ? " + condition + match + //extra +
               // " order by p.nombre_en asc" +
                " limit ? offset ?";
        return list(query, userId, search, offset, limit);
    }

    private static String refine(String search){
        if(search != null && !search.isEmpty()) {
            String s = "";
    //        String s = ">(+\""+ search +"\") <(";
            String[] words = search.split(" ");
            for(String word : words){
                if(word.length()>0)
                    s += " +" + word;
            }
            s += " \""+ search +"\"";
            search = s;
        }
        return  search;
    }

    private static List<Movie> list(String query, Integer userId,  String search, Integer offset, Integer limit){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        List<Movie> movies = new ArrayList<>();
        Movie movie;
        int i = 1;

        try{
            connection = DB.getConnection();
            stmt = connection.prepareStatement(query);
            stmt.setInt(i++, userId);
            if(search != null && !search.isEmpty()) stmt.setString(i++, refine(search));
            stmt.setInt(i++, limit);
            stmt.setInt(i++, offset);
            rs = stmt.executeQuery();

            while(rs.next()){
                movies.add(movie = new Movie(rs.getInt("pelicula_id"), rs.getString("nombre_en"), rs.getInt("anio")));
                movie.director = rs.getString("director");
                movie.country = rs.getString("pais");
                movie.rating = rs.getDouble("puntuacion");
                movie.nameEs = rs.getString("nombre_es");
                movie.cast = rs.getString("reparto");
                movie.img = "img-"+movie.id+"-large.jpg.png";
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
        return movies;
    }

    public static TreeSet<Movie> recommendations2(User user, Map<Long, User> users){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        TreeSet<Movie> movies = new TreeSet<>(new Comparator<Movie>(){
            public int compare(Movie a, Movie b){
                return (int) (a.rating - b.rating);
            }
        });

        try{

            Long movieId = null;
            Movie movie = new Movie();
            User user1;

            connection = DB.getConnection();
            stmt = connection.prepareStatement("SELECT pp.usuario_id, pp.pelicula_id, pp.puntuacion " +
                " FROM pelicula_puntuacion pp" +
                " INNER JOIN ( " +
                " select usuario1_id usuario_id from afinidad where usuario2_id = ? " +
                " union" +
                " select usuario2_id usuario_id from afinidad where usuario1_id = ?" +
                " ) a" +
                " ON a.usuario_id = pp.usuario_id" +
                " LEFT JOIN pelicula_puntuacion b" +
                " ON b.pelicula_id = pp.pelicula_id AND b.usuario_id = ?" +
                " WHERE b.pelicula_id is null ORDER BY pp.pelicula_id");
            stmt.setLong(1, user.id);
            stmt.setLong(2, user.id);
            stmt.setLong(3, user.id);

            rs = stmt.executeQuery();
            while(rs.next()){
                if(movieId != rs.getLong("pelicula_id")) {
                    movies.add(movie = new Movie());
                }
                if((user1 = users.get(rs.getLong("usuario_id"))) != null){
                    movie.rating += Math.pow(user1.affinity * rs.getInt("puntuacion"),2);
                }
            }
            rs.close();

        }catch (Exception ex){

        }finally{
            try{
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            }catch (Exception e){}
        }
        return movies;
    }

    @Override
    public String toString(){
        return String.valueOf(this.id);
    }

    public static List<Movie> topRated(Integer nItems){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        List<Movie> movies = new ArrayList<>();
        Movie movie;

        try{
            connection = DB.getConnection();
            stmt = connection.prepareStatement("select p.*, count(pp.pelicula_id) n from pelicula_puntuacion pp" +
                    " inner join pelicula p" +
                    " on pp.pelicula_id = p.pelicula_id" +
                    " group by pp.pelicula_id" +
                    " order by n desc limit ? offset 0");
            stmt.setInt(1, nItems);
            rs = stmt.executeQuery();

            while(rs.next()){
                movies.add(movie = new Movie(rs.getInt("pelicula_id"), rs.getString("nombre_en"), rs.getInt("anio")));
                movie.director = rs.getString("director");
                movie.country = rs.getString("pais");
                movie.img = "img-"+rs.getString("pelicula_id")+"-large.jpg.png";
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
        return movies;
    }

    public static List<Movie> bestRated(Integer nItems){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        List<Movie> movies = new ArrayList<>();
        Movie movie;

        try{
            connection = DB.getConnection();
            stmt = connection.prepareStatement("select p.pelicula_id, p.nombre_en, p.anio, p.pais, p.director, p.nombre_es, " +
                    " sum(pp.puntuacion) suma from pelicula p" +
                    " inner join pelicula_puntuacion pp" +
                    " on pp.pelicula_id = p.pelicula_id" +
                    " group by p.pelicula_id, p.nombre_en, p.anio, p.pais, p.director, p.nombre_es" +
                    " order by suma desc" +
                    " limit ? offset 0");
            stmt.setInt(1, nItems);
            rs = stmt.executeQuery();

            while(rs.next()){
                movies.add(movie = new Movie(rs.getInt("pelicula_id"), rs.getString("nombre_en"), rs.getInt("anio")));
                movie.director = rs.getString("director");
                movie.country = rs.getString("pais");
                movie.img = "img-"+rs.getString("pelicula_id")+"-large.jpg.png";
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
        return movies;
    }


    public static List<Movie> recommendations(IdentityUser user, Map<Integer, IdentityUser> users){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        List<Movie> movies = new ArrayList<>();

        try{

            Integer movieId = -1;
            Movie movie = new Movie();
            IdentityUser user1;
            boolean more = true;

            connection = DB.getConnection();
            stmt = connection.prepareStatement("select pa.pelicula_id, pa.usuario_id, pa.puntuacion from pelicula_puntuacion pa" +
                    " left join pelicula_puntuacion pb" +
                    " on pa.pelicula_id = pb.pelicula_id and pb.usuario_id = ?" +
                    " where pa.usuario_id <> ? and pb.usuario_id is null" +
                    " order by pelicula_id");
            stmt.setInt(1, user.id);
            stmt.setInt(2, user.id);

            rs = stmt.executeQuery();
            while(rs.next()){
                if(rs.getInt("pelicula_id") != movieId) {
                    movies.add(movie = new Movie());
                    movie.id = movieId = rs.getInt("pelicula_id");
                    movie.img = "0";
                }
                if((user1 = users.get(rs.getInt("usuario_id"))) != null){
                    movie.rating += user1.affinity * (rs.getDouble("puntuacion")/5d);
//                    movie.rating += Math.pow(user1.affinity * rs.getInt("puntuacion"),2);
                    //movie.img = String.valueOf(Double.parseDouble(movie.img) + (user1.affinity * rs.getInt("puntuacion")));
                    System.out.println(rs.getString("pelicula_id")+" puntuacion: "+rs.getString("puntuacion")+ " afinity " + user1.affinity);
                }
            }
            rs.close();
            stmt.close();

            Collections.sort( movies, new Comparator<Movie>(){
                public int compare(Movie a, Movie b){
//                    System.out.println(a.rating + " " + b.rating + "  = "+ a.rating.compareTo(b.rating));
                    return (a.rating.compareTo(b.rating))*-1;
                }
            });

            for(Movie m:movies){
                System.out.println("  -->  "+m.id+"  rating "+m.rating);
            }

            String ids = movies.toString();
            ids = ids.substring(1,ids.length()-2).replace(", ","', '");

            System.out.println(ids);


            stmt = connection.prepareStatement("select p.* from pelicula p where p.pelicula_id " +
                    " in ('"+ids+"') order by field (p.pelicula_id, '"+ids+"')");
            //stmt.setString(1, ids);
            //stmt.setString(2, ids);
            rs = stmt.executeQuery();

            System.out.println(rs.getFetchSize());
            if(rs.next())
                for(Movie m : movies){
                    if(m.id == rs.getInt("pelicula_id")) {
                        m.director = rs.getString("director");
                        m.country = rs.getString("pais");
                        m.name = rs.getString("nombre_en");
                        m.year = rs.getInt("anio");
                        m.img = "img-"+m.id+"-large.jpg.png";
                        more = rs.next();
                        if(more == false)break;
                    }

                }

        }catch (Exception ex){
            ex.printStackTrace();
        }finally{
            try{
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            }catch (Exception e){}
        }




        return movies;
    }

    public static boolean rate(Integer userId, Integer movieId, float rating){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        boolean success = false;

        try{
            connection = DB.getConnection();
            stmt = connection.prepareStatement("INSERT INTO pelicula_puntuacion" +
                    " (usuario_id, pelicula_id, puntuacion)" +
                    " VALUES (?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE puntuacion = ?");
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            stmt.setFloat(3, rating);
            stmt.setFloat(4, rating);
            stmt.executeUpdate();
            success = true;

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            }catch (Exception e){}
        }
        return success;
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof Movie){
            Movie m = (Movie) o;
            if(this.id == m.id) return true;
        }
        return false;
    }

    public static Movie get(Integer id){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Movie movie = null;

        try{
            connection = DB.getConnection();
            stmt = connection.prepareStatement("select pelicula_id, nombre_es, nombre_en, director, guion, musica, anio, pais, reparto, tipo" +
                    " from pelicula where pelicula_id = ?");
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            while(rs.next()){
                movie = new Movie();
                movie.id = rs.getInt("pelicula_id");
                movie.name = rs.getString("nombre_en");
                movie.nameEs = rs.getString("nombre_es");
                movie.director = rs.getString("director");
                movie.year = rs.getInt("anio");
                movie.country = rs.getString("pais");
                movie.cast = rs.getString("reparto");
                movie.releaseDate = new Date();
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
        return movie;
    }


//    public static void save(Movie movie){
//
//        Connection connection = null;
//        PreparedStatement stmt = null;
//        ResultSet rs = null;
//
//        String[] keys = {"pelicula_id"};
//       // Integer userId = find(identity);
//        IdentityUser user = null;
//
//        try{
//            connection = DB.getConnection();
//            stmt = connection.prepareStatement("insert into usuario (usuario_id, email, nombre, ultimo_acceso, password, oauth_proveedor, oauth_usuario)" +
//                    " values (?, ?, ?, CURRENT_TIMESTAMP, null, ?, ?)" +
//                    "  ON DUPLICATE KEY UPDATE ultimo_acceso = CURRENT_TIMESTAMP", keys);
//
//            if(userId!=null)
//                stmt.setInt(1, userId);
//            else {
//                stmt.setNull(1, Types.INTEGER);
//            }
//            if(identity.email() != null)
//                stmt.setString(2, identity.email().toString());
//            else
//                stmt.setNull(2, Types.VARCHAR);
//
//            stmt.setString(3, identity.fullName());
//            stmt.setString(4, identity.identityId().providerId());
//            stmt.setString(5, identity.identityId().userId());
//            stmt.executeUpdate();
//            rs = stmt.getGeneratedKeys();
//
//            if(rs.next()){
//                user = new IdentityUser();
//                user.identityId = identity.identityId();
//                user.email = identity.email();
//                user.fullName = identity.fullName();
//                user.firstName = identity.firstName();
//                user.lastName = identity.lastName();
//                user.id = rs.getInt(1);
//            }
//
//        } catch(Exception e){
//            e.printStackTrace();
//        }finally {
//            try{
//                if (rs != null) rs.close();
//                if (stmt != null) stmt.close();
//                if (connection != null) connection.close();
//            }catch (Exception e){}
//        }
//        return user;
//
//    }


}
