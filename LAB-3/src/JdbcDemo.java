import java.sql.*;

public class JdbcDemo {

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        Connection con = null;
        try {
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/college", "root", "mydevsql");
        } catch (SQLException e) {
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/college", "root", "root");
        }

        PreparedStatement ps = con.prepareStatement("insert into student values(?,?,?)");
        ps.setInt(1, 101);
        ps.setString(2, "Udiksha");
        ps.setString(3, "CSE");

        try {
            ps.executeUpdate();
            System.out.println("Record Inserted");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Record with id 101 already exists");
        }

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("select * from student");

        while (rs.next()) {
            System.out.println(rs.getInt(1) + " " + rs.getString(2) + " " + rs.getString(3));
        }

        con.close();
    }
}
