package models;


import play.db.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class Movie {

    public Integer id;
    public String name;
    public Integer year;
    public Double rating = 0.0;
    public String country;
    public String director;
    public String img ;//=  "http://lorempixel.com/160/230/people/";

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

        query = "SELECT p.pelicula_id, p.guion, p.director, p.nombre_en, p.anio, p.pais, pp.puntuacion" +
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



    public static List<Movie> recommendations(User user, Map<Long, User> users){

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        List<Movie> movies = new ArrayList<>();

//        TreeSet<Movie> movies = new TreeSet<>(new Comparator<Movie>(){
//            public int compare(Movie a, Movie b){
//                return (int) (a.rating - b.rating);
//            }
//        });
//
//        Map<Integer, Movie> moviess = new HashMap<> ();

        try{

            Integer movieId = -1;
            Movie movie = new Movie();
            User user1;

            connection = DB.getConnection();
            stmt = connection.prepareStatement("select pa.pelicula_id, pa.usuario_id, pa.puntuacion from pelicula_puntuacion pa" +
                    " left join pelicula_puntuacion pb" +
                    " on pa.pelicula_id = pb.pelicula_id and pb.usuario_id = ?" +
                    " where pa.usuario_id <> ? and pb.usuario_id is null" +
                    " order by pelicula_id");
            stmt.setLong(1, user.id);
            stmt.setLong(2, user.id);

            rs = stmt.executeQuery();
            while(rs.next()){
                if(rs.getInt("pelicula_id") != movieId) {
                    movies.add(movie = new Movie());
                    movie.id = movieId = rs.getInt("pelicula_id");
                    movie.img = "0";
                }
                if((user1 = users.get(rs.getLong("usuario_id"))) != null){
                    movie.rating += Math.pow(user1.affinity * rs.getInt("puntuacion"),2);
                    movie.img = String.valueOf(Double.parseDouble(movie.img) + (user1.affinity * rs.getInt("puntuacion")));
                    System.out.println(rs.getString("pelicula_id")+" puntuacion: "+rs.getString("puntuacion")+ " afinity " + user1.affinity);
                }
            }
            rs.close();
            stmt.close();

            Collections.sort( movies, new Comparator<Movie>(){
                public int compare(Movie a, Movie b){
                    return (int) (a.rating - b.rating);
                }
            });

//            TreeMap<Integer, Movie> movies2 = new TreeMap<>(new ValueComparator(moviess));
//            movies2.descendingMap().

            /*
            * select p.* from pelicula p where p.pelicula_id in (100046, 108781, 106172)
order by field(p.pelicula_id, '100046','106172', '108781');
            * */


            for(Movie m : movies){
                System.out.println(m.id + "  " + m.rating+ "  normal "+m.img);
            }

            System.out.println(movies.toString());


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

}
