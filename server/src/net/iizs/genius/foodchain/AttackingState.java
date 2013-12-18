package net.iizs.genius.foodchain;

import static net.iizs.genius.foodchain.Constants.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class AttackingState extends AbstractGameRoomState {
	private static final Logger logger_ = Logger.getLogger(FoodChainServerHandler.class.getName());

	private boolean forceProceed_;
	private boolean invincibleHerbivores_;
	
	public AttackingState(AbstractGameRoomState cl) {
		super(cl);

		forceProceed_ = false;
		invincibleHerbivores_ =  false;
		
		// 플레이어 이동. 진행 동의 초기화
		Set<Area> herviAreas = new HashSet<Area>(); 
		for ( Player p: players_.values() ) {
			if ( p.isAlive() ) {
				minimap_.get(p.getCurrentArea()).remove(p);
				p.setCurrentArea(p.getMoves().get(round_ - 1));
				minimap_.get(p.getCurrentArea()).add(p);
				
				p.setPassed(false);
				if ( herbivores_.contains(p) ) {
					herviAreas.add( p.getCurrentArea() );
				}
			}
		}
		
		if ( herviAreas.size() == 1 ) {
			invincibleHerbivores_ = true;
		}
		
		jobQueue_.add(new ScheduleRequest("/admin " + adminPassword_ + " end " + Integer.toString(round_)
						, ATTACK_TIME_LIMIT_SECOND * 1000));
		
		broadcast( round_ + " 라운드가 시작되었습니다.");
		broadcast("플레이어들의 이동결과는 다음과 같습니다.");
		broadcast("");
		broadcast( Area.PLAINS.getName() + ": " + minimap_.get(Area.PLAINS).toString() );
		broadcast( Area.WOODS.getName() + ": " + minimap_.get(Area.WOODS).toString() );
		broadcast( Area.SKY.getName() + ": " + minimap_.get(Area.SKY).toString() );
		broadcast( Area.RIVER.getName() + ": " + minimap_.get(Area.RIVER).toString() );
		broadcast("");
		broadcast("각 플레이어들은 /a [닉네임] 명령으로 같은 지역에 있는 플레이어를 공격할 수 있습니다.");
		broadcast("공격 결과는 즉시 공개됩니다. 제한 시간은 " + ( ATTACK_TIME_LIMIT_SECOND / 60 ) + "분 입니다.");
		broadcast("공격 결과 판정 순서는 *입력 순서대로* 입니다.");
		broadcast("더 이상 공격을 하지 않으려 한다면 /pass 명령을 입력해주세요.");
		broadcast("모든 플레이어가 /pass 명령을 입력한다면, " + ( ATTACK_TIME_LIMIT_SECOND / 60 ) + "분이 지나지 않아도 다음 라운드로 진행됩니다.");
		
		logger_.info("@" + name_ + ": " + round_ + " starts");
		logger_.info("@" + name_ + ": " + Area.PLAINS + ": " + minimap_.get(Area.PLAINS).toString());
		logger_.info("@" + name_ + ": " + Area.WOODS + ": " + minimap_.get(Area.WOODS).toString());
		logger_.info("@" + name_ + ": " + Area.SKY + ": " + minimap_.get(Area.SKY).toString());
		logger_.info("@" + name_ + ": " + Area.RIVER + ": " + minimap_.get(Area.RIVER).toString());
	}
	
	private AbstractGameRoomState proceed() throws Exception {
		boolean flag = true;
		
		for ( Player p: players_.values() ) {
			if ( p.isBot() ) continue; 
			
			if ( p.isAlive() ) {
				if ( p.getPassed() != true ) {
					flag = false;
					break;
				}
			}
		}
		
		if ( flag == true || forceProceed_ == true ) {
			// 다음 라운드로 진행
			broadcast( round_ + " 라운드가 종료되었습니다.");
			
			// 굶어죽은 경우 확인
			for ( Player p: players_.values() ) {
				if ( p.isAlive() ) {
					Character c = p.getCharacter();
					
					if ( c.equals(Character.LION) 
							&& ( p.getRoundsAte().size() != round_ ) ) {
						kill(p);
						continue;
					}
					
					if ( c.equals(Character.CROCODILE) 
							&& ( p.getRoundsAte().size() < ( round_ - 1 )  ) ) {
						kill(p);
						continue;
					}
					
					if ( c.equals(Character.EAGLE) 
							&& ( p.getRoundsAte().size() < ( round_ - 1 )  ) ) {
						kill(p);
						continue;
					}
					
					if ( c.equals(Character.HYENA) 
							&& ( p.getRoundsAte().size() < ( round_ - 2 )  ) ) {
						kill(p);
						continue;
					}
				}
			}
			
			if ( round_ < MAX_GAME_ROUND ) {				
				return new MovingState(this);
			} else {
				// 게임 종료
				broadcast("게임이 종료되었습니다.");
				List<Player> winner = new ArrayList<Player>();
				for ( Player p: players_.values() ) {
					Character c = p.getCharacter();
					
					if ( c.equals( Character.RAT ) ) {
						if ( charmap_.get(Character.LION).isAlive() ) {
							winner.add(p);
						}
					} else if ( c.equals( Character.EGYPTIAN_PLOVER ) ) {
						if ( charmap_.get(Character.CROCODILE).isAlive() ) {
							winner.add(p);
						}
					} else if ( c.equals( Character.CROW ) ) {
						if ( charmap_.get(p.getSelection()).isAlive() ) {
							winner.add(p);
						}
					} else if ( c.equals( Character.HYENA ) ) {
						if ( ! charmap_.get(Character.LION).isAlive() ) {
							winner.add(p);
						}
					} else if ( c.equals( Character.SNAKE ) ) {
						if ( kills_ >= KILLS_SNAKE_TO_WIN ) {
							winner.add(p);
						}
					} else {
						if ( p.isAlive() ) {
							winner.add(p);
						}
					}
				}
				
				broadcast("");
				for ( Player p: players_.values() ) {
					broadcast( "[" + p.getNickname() + "] = '" + p.getCharacter().getName() + "'" );
				}
				broadcast("");
				broadcast("우승자는 " + winner.toString() + "입니다.");
				
				logger_.info("Winners: " + winner.toString());
				
				return new WaitingState(this);
			}
		}
		
		return this;
	}
	
	private void broadcastToArea(Area a, String msg) {
        for (Player p: minimap_.get(a)) {
        	if ( p.isBot() ) { continue; }
        	p.getChannel().writeAndFlush( "===== " + msg + NEWLINE);
        }
	}
	
	private void kill(Player p) {
		p.kill();
		++kills_;
		broadcast("[" + p.getNickname() + "]님이 죽었습니다.");
		minimap_.get(p.getCurrentArea()).remove(p);
		minimap_.get(Area.HALL).add(p);
		p.setCurrentArea(Area.HALL);
		logger_.info("@" + name_ + ": " + "[" + p.getNickname() + "] is killed");
	}
	
	// a 가 d 를 공격
	private void attack(Player a, Player d) throws Exception {
		Character ac = a.getCharacter();
		Character dc = d.getCharacter();
		
		if ( a.equals(d) ) {
			throw new GeniusServerException("자기 자신은 공격할 수 없습니다.");
		}
		
		if ( ac.equals(Character.SNAKE) ) {
			throw new GeniusServerException("'" + ac.getName() + "'은 공격할 수 없습니다.");
		}
		
		if ( ! a.getCurrentArea().equals(d.getCurrentArea()) ) {
			throw new GeniusServerException("같은 지역의 플레이어만 공격할 수 있습니다.");
		}
		
		// 사실 이 메시지는 나오면 안됨. 죽으면 지역을 빠져나가기 때문
		if ( d.isAlive() == false ) {
			throw new GeniusServerException("살아있는 플레이어만 공격할 수 있습니다.");
		}
		
		// 여기까지 왔으면, 유효한 공격임
		broadcastToArea(a.getCurrentArea(), "[" + a.getNickname() + "]님이 [" + d.getNickname() + "]님을 공격했습니다.");	
		
		if ( ac.getRank() <= dc.getRank() ) { 
			broadcastToArea(a.getCurrentArea(), "아무일도 일어나지 않았습니다.");
			return; 
		}
		
		// 여기까지 왔다는 것은 공격자가 더 강하다는 뜻.
		// 특수 조건만 살피면 된다.
		if ( dc.equals(Character.SNAKE) ) {
			// 청둥오리, 토끼, 사슴, 수달 의 무적조건과 충돌하는데, 
			// 일단 뱀의 특수조건을 상위로 올려두었음
			kill(a);
		}
		
		if ( invincibleHerbivores_ 
				&& ( dc.equals(Character.MALLARD)
						|| dc.equals(Character.RABBIT)
						|| dc.equals(Character.DEER)
						|| dc.equals(Character.OTTER) )  ) {
			broadcastToArea(a.getCurrentArea(), "아무일도 일어나지 않았습니다.");
			return; 
		}
		
		kill(d);
		a.eat(d, round_);
	}

	@Override
	public synchronized AbstractGameRoomState userCommand(String nickname, String req)
			throws Exception {
		String cmds[] = req.split("\\s+", 3);
    	String cmd = cmds[0].toLowerCase();
    	
    	if ( cmd.equals("/a") ) {
    		Player p = getPlayer(nickname);
    		
    		if ( cmds.length < 2 ) {
    			throw new GeniusServerException("공격대상을 지정해야 합니다.");
    		}
    		
    		Player t = getPlayer(cmds[1]);
    		
    		attack( p, t );
    	} else if ( cmd.equals("/pass") ) {
    		Player p = getPlayer(nickname);
    		
    		p.setPassed(true);
    		p.getChannel().writeAndFlush(">>> 다음 라운드로 진행해도 좋다고 설정하셨습니다." + NEWLINE );    		
    	} else if ( cmd.equals("/admin") ) {
    		cmds = req.split("\\s+");
        	
    		if ( cmds.length < 4 ) {
    			throw new GeniusServerException("/admin [관리자암호] end [라운드]");
    		}

    		if ( ! cmds[1].equals(adminPassword_) ) {
    			throw new GeniusServerException("관리자 암호 오류");
    		}
    		
    		if ( cmds[2].toLowerCase().equals("end") ) {
	    		if ( Integer.parseInt( cmds[3] ) != round_ ) {
	    			throw new GeniusServerException("진행중인 라운드가 아닙니다.");
	    		}
	    		forceProceed_ = true;
    		}
    		
    	} else if ( cmd.equals("/to") ) {
    		whisper(nickname, cmds[1], cmds[2]);
    	} else if ( cmd.equals("/info") ) {
    		showInfo( nickname );
    	} else {
    		printUsageSimple(nickname);
    	}
    	
    	return proceed();
	}

	@Override
	public void printUsageSimple(String nickname) throws Exception {
		getPlayer(nickname).getChannel().writeAndFlush(ATTACK_USAGE_SIMPLE + NEWLINE);
	}

	@Override
	public void showInfo(String nickname) throws Exception {
		Player p = getPlayer(nickname);
		
		p.getChannel().write( "> 방 번호: " + name_ + NEWLINE );
		//p.getChannel().write( "> 관리자암호: " + adminPassword_ + NEWLINE );
		p.getChannel().write( "> 플레이어" + NEWLINE );
		
    	Set<String> playerNames = players_.keySet();
    	Iterator<String> iter = playerNames.iterator();
    	while ( iter.hasNext() ) {    		
    		Player i = getPlayer(iter.next());
    		
    		p.getChannel().write("> [" + i.getNickname() + "]");
    		if ( i.isBot() ) {
    			p.getChannel().write( " (Bot)");
    		}
    		p.getChannel().write(": ");
    		p.getChannel().write( i.getMoves().subList(0, round_ - 1).toString() );
    		p.getChannel().write(": " + ( i.isAlive() ? "생존" : "죽음" ) );
    		p.getChannel().write( NEWLINE );
    	}
    	
    	p.getChannel().write( Area.PLAINS.getName() + ": " + minimap_.get(Area.PLAINS).toString() + NEWLINE );
    	p.getChannel().write( Area.WOODS.getName() + ": " + minimap_.get(Area.WOODS).toString() + NEWLINE );
    	p.getChannel().write( Area.SKY.getName() + ": " + minimap_.get(Area.SKY).toString() + NEWLINE );
		p.getChannel().write( Area.RIVER.getName() + ": " + minimap_.get(Area.RIVER).toString() + NEWLINE  );
    	
    	p.getChannel().flush();
	}
	
	@Override
	public void chat(String nickname, String msg) throws Exception {
		Player p = getPlayer(nickname);
        for (Player to: minimap_.get(p.getCurrentArea())) {
        	if ( to.isBot() ) continue;
        	to.getChannel().writeAndFlush("[" + nickname + "] " + msg + NEWLINE);
        }
	}
	
	@Override
	public void whisper(String nickname, String to, String msg) throws Exception {
		Player p = getPlayer(to);
		Player me = getPlayer(nickname);
		
		if ( p.isBot() ) {
			throw new GeniusServerException("[" + to + "]님은 봇입니다.");
		}
		
		if ( ! p.getCurrentArea().equals(me.getCurrentArea() ) ) {
			throw new GeniusServerException("다른 지역에 있는 플레이어에게는 귓속말을 보낼 수 없습니다.");
		}
		
		p.getChannel().writeAndFlush(">>> [" + nickname + "]님의 귓속말: " + msg + NEWLINE);
		me.getChannel().writeAndFlush( ">>> [" + to + "]님께 귓속말을 보냈습니다." + NEWLINE);
	}

}
