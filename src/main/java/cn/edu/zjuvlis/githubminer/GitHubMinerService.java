package cn.edu.zjuvlis.githubminer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.WatcherService;

public class GitHubMinerService {
	private LinkedList<UserSimple> seedDict = new LinkedList<UserSimple>();
	private LinkedList<RepoSimple> repoDict = new LinkedList<RepoSimple>();
	private HashMap<UserSimple, LinkedList<RepoSimple>> correDict = new HashMap<UserSimple, LinkedList<RepoSimple>>(); 
	private File seed = new File("/roy/test/python_test/machineLearning/datasets/users_dict.txt");
	private File desti = new File("/roy/mlDataSets/corre_dict.dat");
	private GitHubClient client;
	public GitHubMinerService(){
		client = new GitHubClient();
		client.setCredentials("roygao", "gao000628");
		importSeed();
	}
	public LinkedList<UserSimple> getSeed(){
		return seedDict;
	}
	public void importSeed(){
		try {
			System.out.println("Start loading Seeds！！！ ");
			long counter = 0;
			String str = null;
			FileReader rd = new FileReader(seed);
			BufferedReader br = new BufferedReader(rd);
			while((str = br.readLine()) != null ){
				counter ++;
				seedDict.add(new UserSimple(str, counter));
			}
			rd.close();
			br.close();
			System.out.println("Loading Finished ! ! ! ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void correGenerator(String option){
		for(UserSimple usr : seedDict){
			System.out.println("==============================");
			switch(option){
			case "star":
				correDict.put(usr, getStarers(usr));
				break;
			case "fork":
				correDict.put(usr, getForkers(usr));
				break;
			default:
				System.out.println("No this option supported");
				break;
			}
		}
	}
	public LinkedList<RepoSimple> getForkers(UserSimple user){
		RepositoryService rs = new RepositoryService(this.client);
		LinkedList<RepoSimple> listRepo = new LinkedList<RepoSimple>();
		System.out.println("Getting forked repos of" + (user.getName()));
		try{
			for(Iterator<Repository> i = rs.getRepositories().iterator(); i.hasNext();){
				Repository rep = i.next();
				if(rep.isFork() == true)
					listRepo.add(new RepoSimple(rep.getName(), rep.getId()));
			}
			System.out.println("finish forked getting repos of" + (user.getName()));
		}catch(IOException e){
			e.printStackTrace();
		}
		return listRepo;
	}
	public LinkedList<RepoSimple> getStarers(UserSimple user){
		WatcherService ws = new WatcherService(this.client);
		LinkedList<RepoSimple> listRepo = new LinkedList<RepoSimple>();
		System.out.println("Getting starred repos of" + (user.getName()));
		try{
			for(Iterator<Repository> i = ws.getWatched(user.getName()).iterator(); i.hasNext();){
				Repository rep = i.next();
				listRepo.add(new RepoSimple(rep.getName(), rep.getId()));
			}
			System.out.println("finish starred getting repos of" + (user.getName()));
		}catch (IOException e){
			e.printStackTrace();
		}
		return listRepo;
	}
	public void flushCorrelation(){
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter(desti);
			bw = new BufferedWriter(fw);
			Iterator<Entry<UserSimple, LinkedList<RepoSimple>>> iter = correDict.entrySet().iterator();
			StringBuffer sb = new StringBuffer();
			System.out.println("Start flushing Correlations ！！！ ");
			while(iter.hasNext()){
				Map.Entry<UserSimple, LinkedList<RepoSimple>> entry = iter.next();
				sb.append(entry.getKey().getUserId());
				for(Iterator<RepoSimple> i = entry.getValue().iterator(); i.hasNext();){
					RepoSimple rep = i.next();
					sb.append(" " + rep.getRepoID());
				}
				sb.append("\r\n");
				bw.write(sb.toString());
				sb.delete(0, sb.length());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				fw.close();
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
 		GitHubMinerService miner = new GitHubMinerService();
		miner.correGenerator("fork");
		miner.flushCorrelation();
	}
}

class RepoSimple {
	private String strRepoName;
	private long strRepoId;
	public RepoSimple(String name, long id){
		strRepoName = name;
		strRepoId = id;
	}
	public long getRepoID(){
		return strRepoId;
	}
	public String getRepoName(){
		return strRepoName;
	}
}

class UserSimple {
	private String strUserName;
	private long strUserId;
	public UserSimple(String name,long id){
		strUserName = name;
		strUserId = id;
	}
	public long getUserId(){
		return strUserId;
	}
	public String getName(){
		return strUserName;
	}
} 
