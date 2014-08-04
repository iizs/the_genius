package net.iizs.genius.server.foodchain;

import static net.iizs.genius.server.foodchain.FoodChainConstants.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.iizs.genius.server.GeniusServerHandler;
import net.iizs.genius.server.GeniusServerException;
import net.iizs.genius.server.KeyValueResponse;
import net.iizs.genius.server.Player;
import net.iizs.genius.server.ScheduleRequest;
import net.iizs.genius.server.SimpleResponse;

public class FoodChainAttackState extends AbstractFoodChainState {
	private static final Logger logger_ = Logger.getLogger(GeniusServerHandler.class.getName());

	private boolean forceProceed_;
	private boolean invincibleHerbivores_;
	
	public FoodChainAttackState(AbstractFoodChainState cl) {
		super(cl);

		forceProceed_ = false;
		invincibleHerbivores_ =  false;
		
		// 플레이어 이동. 진행 동의 초기화
		Set<FoodChainArea> herviAreas = new HashSet<FoodChainArea>(); 
		for ( Player ap: getAllPlayers().values() ) {
			FoodChainPlayer p = (FoodChainPlayer) ap;
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
		
		getJobQueue().add(new ScheduleRequest("/admin " + getAdminPassword() + " end " + Integer.toString(round_)
						, ATTACK_TIME_LIMIT_SECOND * 1000));
		
		broadcast( getMessage( "attackStateGuide"
						, round_
						, ( getName( FoodChainArea.PLAINS ) + ": " + minimap_.get(FoodChainArea.PLAINS).toString() )
						, ( getName( FoodChainArea.WOODS ) + ": " + minimap_.get(FoodChainArea.WOODS).toString() )
						, ( getName( FoodChainArea.SKY ) + ": " + minimap_.get(FoodChainArea.SKY).toString() )
						, ( getName( FoodChainArea.RIVER ) + ": " + minimap_.get(FoodChainArea.RIVER).toString() )
						, ( ATTACK_TIME_LIMIT_SECOND / 60 ) ) );
		
		logger_.info("@" + getName() + ": " + round_ + " starts");
		logger_.info("@" + getName() + ": " + FoodChainArea.PLAINS + ": " + minimap_.get(FoodChainArea.PLAINS).toString());
		logger_.info("@" + getName() + ": " + FoodChainArea.WOODS + ": " + minimap_.get(FoodChainArea.WOODS).toString());
		logger_.info("@" + getName() + ": " + FoodChainArea.SKY + ": " + minimap_.get(FoodChainArea.SKY).toString());
		logger_.info("@" + getName() + ": " + FoodChainArea.RIVER + ": " + minimap_.get(FoodChainArea.RIVER).toString());
	}
	
	private AbstractFoodChainState proceed() throws Exception {
		boolean flag = true;
		
		for ( Player ap: getAllPlayers().values() ) {
			FoodChainPlayer p = (FoodChainPlayer) ap;
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
			broadcast( getMessage( "roundEnd", round_ ) );
			
			// 굶어죽은 경우 확인
			for ( Player ap: getAllPlayers().values() ) {
				FoodChainPlayer p = (FoodChainPlayer) ap;
				if ( p.isAlive() ) {
					FoodChainCharacter c = p.getCharacter();
					
					if ( c.equals(FoodChainCharacter.LION) 
							&& ( p.getRoundsAte().size() != round_ ) ) {
						kill(p);
						continue;
					}
					
					if ( c.equals(FoodChainCharacter.CROCODILE) 
							&& ( p.getRoundsAte().size() < ( round_ - 1 )  ) ) {
						kill(p);
						continue;
					}
					
					if ( c.equals(FoodChainCharacter.EAGLE) 
							&& ( p.getRoundsAte().size() < ( round_ - 1 )  ) ) {
						kill(p);
						continue;
					}
					
					if ( c.equals(FoodChainCharacter.HYENA) 
							&& ( p.getRoundsAte().size() < ( round_ - 2 )  ) ) {
						kill(p);
						continue;
					}
				}
			}
			
			if ( round_ < MAX_GAME_ROUND ) {				
				return new FoodChainMoveState(this);
			} else {
				// 게임 종료
				broadcast( getMessage("gameEnd") );
				List<FoodChainPlayer> winner = new ArrayList<FoodChainPlayer>();
				for ( Player ap: getAllPlayers().values() ) {
					FoodChainPlayer p = (FoodChainPlayer) ap;
					FoodChainCharacter c = p.getCharacter();
					
					if ( c.equals( FoodChainCharacter.RAT ) ) {
						if ( charmap_.get(FoodChainCharacter.LION).isAlive() ) {
							winner.add(p);
						}
					} else if ( c.equals( FoodChainCharacter.EGYPTIAN_PLOVER ) ) {
						if ( charmap_.get(FoodChainCharacter.CROCODILE).isAlive() ) {
							winner.add(p);
						}
					} else if ( c.equals( FoodChainCharacter.CROW ) ) {
						if ( charmap_.get(p.getSelection()).isAlive() ) {
							winner.add(p);
						}
					} else if ( c.equals( FoodChainCharacter.HYENA ) ) {
						if ( ! charmap_.get(FoodChainCharacter.LION).isAlive() ) {
							winner.add(p);
						}
					} else if ( c.equals( FoodChainCharacter.SNAKE ) ) {
						if ( kills_ >= KILLS_SNAKE_TO_WIN ) {
							winner.add(p);
						}
					} else {
						if ( p.isAlive() ) {
							winner.add(p);
						}
					}
				}
				
				//broadcast("");
				for ( Player ap: getAllPlayers().values() ) {
					FoodChainPlayer p = (FoodChainPlayer) ap;
					broadcast( "[" + p.getId() + "] = '" + getName( p.getCharacter() ) + "'" );
				}
				//broadcast("");
				broadcast( getMessage("winner", winner.toString() ) );
				
				logger_.info("Winners: " + winner.toString());
				
				return new FoodChainWaitState(this);
			}
		}
		
		return this;
	}
	
	private void broadcastToArea(FoodChainArea a, String msg) {
        for (FoodChainPlayer p: minimap_.get(a)) {
        	if ( p.isBot() ) { continue; }
        	p.getChannel().writeAndFlush( 
        			getFormatter().formatGameRoomMessage( "((( " + msg +  " )))" ) );
        }
	}
	
	private void kill(FoodChainPlayer p) {
		p.kill();
		++kills_;
		broadcast( getMessage( "killed", p.getId() ) );
		minimap_.get(p.getCurrentArea()).remove(p);
		minimap_.get(FoodChainArea.HALL).add(p);
		p.setCurrentArea(FoodChainArea.HALL);
		logger_.info("@" + getName() + ": " + "[" + p.getId() + "] is killed");
	}
	
	// a 가 d 를 공격
	private void attack(FoodChainPlayer a, FoodChainPlayer d) throws Exception {
		FoodChainCharacter ac = a.getCharacter();
		FoodChainCharacter dc = d.getCharacter();
		
		if ( a.equals(d) ) {
			throw new GeniusServerException( getMessage("eCannotAttackYourself") );
		}
		
		if ( ac.equals(FoodChainCharacter.SNAKE) ) {
			throw new GeniusServerException( getMessage("eSnakeCannotAttack") );
		}
		
		if ( ! a.getCurrentArea().equals(d.getCurrentArea()) ) {
			throw new GeniusServerException( getMessage("eCannotAttackOtherArea") );
		}
		
		// 사실 이 메시지는 나오면 안됨. 죽으면 지역을 빠져나가기 때문
		if ( d.isAlive() == false ) {
			throw new GeniusServerException( getMessage("eCannotAttackDeadPlayer") );
		}
		
		// 여기까지 왔으면, 유효한 공격임
		broadcastToArea(a.getCurrentArea(), getMessage( "attack", a.getId(), d.getId() ) ); 	
		
		if ( ac.getRank() <= dc.getRank() ) { 
			broadcastToArea(a.getCurrentArea(), getMessage("attackResultNothingHappened") );
			return; 
		}
		
		// 여기까지 왔다는 것은 공격자가 더 강하다는 뜻.
		// 특수 조건만 살피면 된다.
		if ( dc.equals(FoodChainCharacter.SNAKE) ) {
			// 청둥오리, 토끼, 사슴, 수달 의 무적조건과 충돌하는데, 
			// 일단 뱀의 특수조건을 상위로 올려두었음
			kill(a);
		}
		
		if ( invincibleHerbivores_ 
				&& ( dc.equals(FoodChainCharacter.MALLARD)
						|| dc.equals(FoodChainCharacter.RABBIT)
						|| dc.equals(FoodChainCharacter.DEER)
						|| dc.equals(FoodChainCharacter.OTTER) )  ) {
			broadcastToArea(a.getCurrentArea(), getMessage("attackResultNothingHappened") );
			return; 
		}
		
		kill(d);
		a.eat(d, round_);
	}

	@Override
	public synchronized AbstractFoodChainState userCommand(Player player, String[] cmds)
			throws Exception {
		//String cmds[] = req.split("\\s+", 3);
    	String cmd = cmds[0].toLowerCase();
    	
    	if ( cmd.equals("/a") ) {
    		FoodChainPlayer p = getFoodChainPlayer(player.getId());
    		
    		if ( cmds.length < 2 ) {
    			printUsage(player);
    		} else {
	    		FoodChainPlayer t = getFoodChainPlayer(cmds[1]);
	    		
	    		attack( p, t );
    		}
    	} else if ( cmd.equals("/pass") ) {
    		FoodChainPlayer p = getFoodChainPlayer(player.getId());
    		
    		p.setPassed(true);
    		p.getChannel().writeAndFlush( getFormatter().formatResponseMessage(
    				new SimpleResponse( getMessage("passSet") ) ) );    		
    	} else if ( cmd.equals("/admin") ) {
    		//cmds = req.split("\\s+");
        	
    		if ( cmds.length < 4 ) {
    			throw new GeniusServerException("/admin <passcode> end <round_num>");
    		}

    		if ( ! cmds[1].equals(getAdminPassword()) ) {
    			throw new GeniusServerException( getMessage("eInvalidAdminPasscode") );
    		}
    		
    		if ( cmds[2].toLowerCase().equals("end") ) {
	    		if ( Integer.parseInt( cmds[3] ) != round_ ) {
	    			throw new GeniusServerException(getMessage("eNotCurrentRound"));
	    		}
	    		forceProceed_ = true;
    		}
    		
    	} else if ( cmd.equals("/to") ) {
    		whisper(player, cmds[1], cmds[2]);
    	} else if ( cmd.equals("/info") ) {
    		showInfo( player );
    	}
    	
    	return proceed();
	}

	@Override
	public void printUsage(Player player) throws Exception {
		player.getChannel().writeAndFlush(getFormatter().formatResponseMessage(
				new SimpleResponse(getMessage("usageAttackStateSimple"))));
	}
	
	private String movesToString(List<FoodChainArea> moves) {
		String s = "[";
		for ( int i = 0; i < round_ && i < moves.size() ; ++i ) {
			if ( i != 0 ) {
				s += "->";
			}
			s += getName(moves.get(i));
		}
		s += "]";
		return s;
	}

	@Override
	public void showInfo(Player player) throws Exception {
		KeyValueResponse<String, String> resp = new KeyValueResponse<>("");
		
		resp.put( getMessage("iRoomName"), getName() );
		resp.put( getMessage("iRound"), Integer.toString(round_) );
		
		Set<String> playerNames = getAllPlayers().keySet();
    	int cntP = 1;
    	int cntB = 1;
    	Iterator<String> iter = playerNames.iterator();
    	while ( iter.hasNext() ) {
    		FoodChainPlayer p = getFoodChainPlayer(iter.next());
    		if ( ! p.isBot() ) {
    			String s = p.getId() 
    					+ "(" + ( p.isAlive() ? getMessage("iLive") : getMessage("iDead") ) + ")"
    					+ movesToString(p.getMoves()) ;
    			resp.put( getMessage("iPlayerN", cntP), s );
    			++cntP;
    		}
    	}
    	
    	iter = playerNames.iterator();
    	while ( iter.hasNext() ) {
    		FoodChainPlayer p = getFoodChainPlayer(iter.next());
    		if ( p.isBot() ) {
    			String s = p.getId() 
    					+ "(" + ( p.isAlive() ? getMessage("iLive") : getMessage("iDead") ) + ")"
    					+ movesToString(p.getMoves()) ;
    			resp.put( getMessage("iBotN", cntB), s );
    			++cntB;
    		}
    	}
    	
    	for ( FoodChainArea area : minimap_.keySet() ) {
    		if ( area != FoodChainArea.HALL ) {
    			resp.put( getName( area ), minimap_.get(area).toString() );
    		}
    	}
    	
		player.getChannel().writeAndFlush( getFormatter().formatResponseMessage(resp));
	}
	
	
	@Override
	public void chat(Player player, String msg) throws Exception {
		FoodChainPlayer p = getFoodChainPlayer(player.getId());
        for (FoodChainPlayer to: minimap_.get(p.getCurrentArea())) {
        	if ( to.isBot() ) continue;
        	to.getChannel().writeAndFlush( getFormatter().formatChatMessage(p.getId(), msg) );
        }
	}
	
	@Override
	public void whisper(Player player, String to, String msg) throws Exception {
		FoodChainPlayer p = getFoodChainPlayer(to);
		FoodChainPlayer me = getFoodChainPlayer(player.getId());
		
		if ( ! p.getCurrentArea().equals(me.getCurrentArea() ) ) {
			throw new GeniusServerException( getMessage( "eCannotWhisperToOtherArea" ) );
		}
		
		super.whisper(player, to, msg);
	}

}
