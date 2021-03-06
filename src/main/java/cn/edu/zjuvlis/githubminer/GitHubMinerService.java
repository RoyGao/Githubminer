package cn.edu.zjuvlis.githubminer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.CollaboratorService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.WatcherService;



public class GitHubMinerService {
	private LinkedList<UserSimple> seedDict = new LinkedList<UserSimple>();
	private HashMap<String, Long> repoDict = new HashMap<String, Long>();
	private HashMap<UserSimple, LinkedList<RepoSimple>> correDict = new HashMap<UserSimple, LinkedList<RepoSimple>>();
    private ArrayList<GithubAccount> accountDict = new ArrayList<GithubAccount>();
	private File seedUser = new File("/roy/test/python_test/machineLearning/datasets/users_dict.txt");
    private File seedRepo = new File("/roy/test/python_test/machineLearning/datasets/repo_dict_2014_9_2_star.dat");
	private File destiCorr = new File("/roy/mlDataSets/corre_dict.dat");
    private File destiRepo = new File("/roy/mlDataSets/repo_dict.dat");
    private int index = 0; // used to flag the Account of Github
	private GitHubClient client;

    private static final int CORRLIMIT = 2;

	public GitHubMinerService(){
		client = new GitHubClient();
		client.setCredentials("roygao", "zjutest2014");
		importSeed(seedUser);
        importRepo(seedRepo);
        //add account info of Github
        accountDict.add(new GithubAccount("roygao", "zjutest2014"));
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
    private class GithubAccount{
        private String account;
        private String passwd;
        public GithubAccount(String a, String p){
            account = a;
            passwd = p;
        }
        public String getAccount(){ return account; }
        public String getPasswd(){ return passwd; }
    }
    public void importRepo(File seed){
        FileReader rd = null;
        BufferedReader br = null;
        try {
            System.out.println("Start loading Repos！！！ ");
            String str = null;
            rd = new FileReader(seed);
            br = new BufferedReader(rd);
            while((str = br.readLine()) != null ){
                String[] entry = str.split("\\s+");
                repoDict.put(entry[0], Long.valueOf(entry[1]));
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
            case "collaborator":
                correDict.put(usr, getCollaborators(usr));
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
				if(rep.isFork() == true && rep.getWatchers() > CORRLIMIT) {
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
                if(rep.getWatchers() > CORRLIMIT) {
                    String name = rep.getName();
                    if (repoDict.containsKey(name) == false)
                        repoDict.put(name, (long)repoDict.size() + 1);
                    listRepo.add(new RepoSimple(name, repoDict.get(name)));
                }
			}
			System.out.println("finish starred getting repos of " + (user.getName()));
		}catch (IOException e){
			e.printStackTrace();
		}
		return listRepo;
	}

    public LinkedList<RepoSimple> getCollaborators(UserSimple user){
        RepositoryService rs = new RepositoryService(this.client);
        CollaboratorService cs = new CollaboratorService(this.client);
        LinkedList<RepoSimple> listRepo = new LinkedList<RepoSimple>();
        System.out.println("Getting collaborative repos of " + (user.getName()));
        try{
            for(Repository rep : rs.getRepositories(user.getName())){
                if(cs.isCollaborator(rep, user.getName()) && rep.getWatchers() > CORRLIMIT) {
                    String name = rep.getName();
                    if(repoDict.containsKey(name) == false)
                        repoDict.put(name, (long)repoDict.size()+1);
                    listRepo.add(new RepoSimple(name, repoDict.get(name)));
                }
            }
            System.out.println("finish  getting collaborative repos of " + (user.getName()));
        }catch(IOException e){
            e.printStackTrace();
            if(index == accountDict.size()-1)
                index = 0;
            else
                index ++;
            this.client.setCredentials(accountDict.get(index).getAccount(),accountDict.get(index).getPasswd());
            rs = new RepositoryService(this.client);
            cs = new CollaboratorService(this.client);
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
                bw.flush();
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
            System.out.println("Start flushing Repos ！！！ ");
            while(iter.hasNext()){
                Map.Entry<String, Long> entry = iter.next();
                bw.write(entry.getKey() + " " + entry.getValue() + "\r\n");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                bw.flush();
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
