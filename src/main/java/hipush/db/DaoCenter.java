package hipush.db;

import java.sql.SQLException;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class DaoCenter {

	private final static DaoCenter inst = new DaoCenter();

	public static DaoCenter getInstance() {
		return inst;
	}

	private Dao<AppModel, Integer> appDao;
	private Dao<UserModel, String> userDao;
	private Dao<ManagerModel, String> managerDao;
	private Dao<JobModel, String> jobDao;
	private Dao<JobTypeModel, String> jobTypeDao;

	public void initAppDao(ConnectionSource mysqlSource) throws SQLException {
		appDao = DaoManager.createDao(mysqlSource, AppModel.class);
		TableUtils.createTableIfNotExists(mysqlSource, AppModel.class);
	}

	public void initUserDao(ConnectionSource mysqlSource) throws SQLException {
		userDao = DaoManager.createDao(mysqlSource, UserModel.class);
		TableUtils.createTableIfNotExists(mysqlSource, UserModel.class);
	}
	
	public void initManagerDao(ConnectionSource mysqlSource) throws SQLException {
		managerDao = DaoManager.createDao(mysqlSource, ManagerModel.class);
		TableUtils.createTableIfNotExists(mysqlSource, ManagerModel.class);
	}
	
	public void initJobDao(ConnectionSource mysqlSource) throws SQLException {
		jobDao = DaoManager.createDao(mysqlSource, JobModel.class);
		TableUtils.createTableIfNotExists(mysqlSource, JobModel.class);
	}
	
	public void initJobTypeDao(ConnectionSource mysqlSource) throws SQLException {
		jobTypeDao = DaoManager.createDao(mysqlSource, JobTypeModel.class);
		TableUtils.createTableIfNotExists(mysqlSource, JobTypeModel.class);
	}

	public Dao<AppModel, Integer> getAppDao() {
		return appDao;
	}

	public Dao<UserModel, String> getUserDao() {
		return userDao;
	}

	public Dao<ManagerModel, String> getManagerDao() {
		return managerDao;
	}

	public Dao<JobModel, String> getJobDao() {
		return jobDao;
	}

	public Dao<JobTypeModel, String> getJobTypeDao() {
		return jobTypeDao;
	}

}
