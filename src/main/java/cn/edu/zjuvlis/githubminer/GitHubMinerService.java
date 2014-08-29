package cn.edu.zjuvlis.githubminer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.WatcherService;

public class GitHubMinerService {
	private LinkedList<UserSimple> seedDict = new LinkedList<UserSimple>();
	private HashMap<String, Long> repoDict = new HashMap<String, Long>();
	private HashMap<UserSimple, LinkedList<RepoSimple>> correDict = new HashMap<UserSimple, LinkedList<RepoSimple>>(); 
	private File seedUser = new File("/roy/test/python_test/machineLearning/datasets/users_dict.txt");
    private File seedRepo = new File("/roy/test/python_test/machineLearning/datasets/users_dict.txt");
	private File destiCorr = new File("/roy/mlDataSets/corre_dict.dat");
    private File destiRepo = new File("/roy/mlDataSets/repo_dict.dat");
	private GitHubClient client;
	public GitHubMinerService(){
		client = new GitHubClient();
		client.setCredentials("roygao", "gao000628");
		importSeed(seedUser);
//        importSeed(seedRepo);
	}
	public LinkedList<UserSimple> getSeed(){
		return seedDict;
	}
	public void importSeed(File seed){
        FileReader rd = null;
        BufferedReader br = null;
		try {
			System.out.println("Start loading Seeds！！！ ");
			long counter = 0;
			String str = null;
			rd = new FileReader(seed);
			br = new BufferedReader(rd);
			while((str = br.readLine()) != null ){
				counter ++;
				seedDict.add(new UserSimple(str, counter));
			}
			System.out.println("Loading Finished ! ! ! ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            try {
                rd.close();
                br.close();
            }catch (IOException e){
                e.printStackTrace();
            }
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
		System.out.println("Getting forked repos of " + (user.getName()));
		try{
			for(Repository rep : rs.getRepositories(user.getName())){
				if(rep.isFork() == true) {
                    String name = rep.getName();
                    if(repoDict.containsKey(name) == false)
                        repoDict.put(name, (long)repoDict.size()+1);
                    listRepo.add(new RepoSimple(name, repoDict.get(name)));
                }
			}
			System.out.println("finish forked getting repos of " + (user.getName()));
		}catch(IOException e){
			e.printStackTrace();
		}
		return listRepo;
	}
	public LinkedList<RepoSimple> getStarers(UserSimple user){
		WatcherService ws = new WatcherService(this.client);
		LinkedList<RepoSimple> listRepo = new LinkedList<RepoSimple>();
		System.out.println("Getting starred repos of " + (user.getName()));
		try{
			for(Repository rep : ws.getWatched(user.getName())){
                String name = rep.getName();
                if(repoDict.containsKey(name) == false)
                    repoDict.put(name, (long)repoDict.size()+1);
				listRepo.add(new RepoSimple(name, repoDict.get(name)));
			}
			System.out.println("finish starred getting repos of " + (user.getName()));
		}catch (IOException e){
			e.printStackTrace();
		}
		return listRepo;
	}
	public void flushCorrelation(){
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter(destiCorr);
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
    public void flushRepoDict(){
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            fw = new FileWriter(destiRepo);
            bw = new BufferedWriter(fw);
            Iterator<Entry<String, Long>> iter = repoDict.entrySet().iterator();
            System.out.println("Start flushing Correlations ！！！ ");
            while(iter.hasNext()){
                Map.Entry<String, Long> entry = iter.next();
                bw.write(entry.getKey() + "\r\n");
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
		miner.correGenerator("star");
		miner.flushCorrelation();
        miner.flushRepoDict();
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
