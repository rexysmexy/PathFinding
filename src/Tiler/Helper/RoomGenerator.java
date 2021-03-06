package Tiler.Helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import Tiler.Objects.Room;
import Tiler.Objects.Square;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class RoomGenerator {

	/*
	 * This class is dedicated to Procedurally Generating Rooms.
	 * 
	 * @ TODO - Refactor - Split into cleaner methods
	 */

	private Square[][] Nodes;
	private ArrayList<Room> Rooms;
	private TunnelPathFinder TunnelDigger;

	// The Max Number without going outside of the grid and throwing an error.
	private int MaxHeight;
	private int MaxWidth;

	public RoomGenerator(Square[][] Nodes) {

		this.Nodes = Nodes;

		// Room ArrayList is created to store all of the Rooms. There's a rectangle inside of it to help check for
		// Collisions.
		Rooms = new ArrayList<Room>();

		// Gets the Max Size of the Grid. Rooms need to be generated within these Max Values.
		MaxHeight = Nodes[Nodes.length - 1].length - 1;
		MaxWidth = Nodes.length - 1;

		TunnelDigger = new TunnelPathFinder(Nodes);

	}

	public void Generate() {

		// This method generates the rooms.
		GenerateRooms();

		// Makes paths between rooms
		TunnelPathing();

		// Takes the rooms and applies them to the grid.
		TranslatetoGrid();
		System.out.println("There are a total of " + Rooms.size() + " rooms");

	}

	public void GenerateRooms() {
		/*
		 * Try to generate a Random room within the limits of the Nodes Grid.
		 * 
		 * - Needs to be able to test if a room is overlapping another - If theres space between rooms. - Maximum size
		 * of the rooms.
		 */

		Random rand = new Random();

		int MaxRoomSize = 20;
		int MinRoomSize = 10;

		/*
		 * This trying loop keeps on repeating until a certain threshold is reached where there are no more possible
		 * rooms to add within the restrictions.
		 */

		int Trying = 0;

		while (Trying < 1000) {
			// Randomly Generates X,Y Position, and the Size of then room.
			int RandomHeight = rand.nextInt(MaxRoomSize);
			int RandomWidth = rand.nextInt(MaxRoomSize);

			int RandomX = rand.nextInt(MaxWidth - RandomWidth);
			int RandomY = rand.nextInt(MaxHeight - RandomHeight);

			// Contiunes to generate until suitable size is found that is within
			// the Maximum and Minimum value of the room
			while (!(RandomHeight >= MinRoomSize && RandomHeight <= MaxRoomSize)) {
				RandomHeight = rand.nextInt(MaxRoomSize);
			}

			// Contiunes to generate until suitable size is found that is within
			// the Maximum and Minimum value of the room
			while (!(RandomWidth >= MinRoomSize && RandomWidth <= MaxRoomSize)) {
				RandomWidth = rand.nextInt(MaxRoomSize);
			}

			// Checks to see if the room is valid and is within the valid range
			// of the Node Array
			if (RandomX < MaxWidth - RandomWidth && RandomY < MaxHeight - RandomHeight) {

				// Create a temporary room to test for overlapping other rooms.
				// This room is bigger by 1 on all sides.
				Room Temp = new Room(new Rectangle(RandomX - 1, RandomY - 1, RandomWidth + 2, RandomHeight + 2));

				/*
				 * This For loop test got kind of tricky. You cannot simply add the room to the ArrayList and check if
				 * it doesnt overlap. If you do it will add and try to test against itself to remove itself throwing an
				 * error (ConcurrentModificationException).
				 */

				boolean IsOverlapping = false;

				for (Room room : Rooms) {

					// If the room overlaps at all, it sets the boolean as True.
					// Discarding it from being added to the ArrayList
					if (Temp.getRectangle().overlaps(room.getRectangle())) {
						IsOverlapping = true;

					}
				}

				// If the Room didn't end up overlapping add it to the ArrayList
				if (IsOverlapping == false) {
					Rooms.add(new Room(new Rectangle(RandomX, RandomY, RandomWidth, RandomHeight)));

				}

			} else {
				Trying++;
			}

		}

	}
	

	public void ArrayChange() {

		// This is nothing more than an experiment. I wanted it to order the ArrayList to go from unsorted to sorted
		// from closest to farthest.

		Room MainRoom = Rooms.get(0);

		@SuppressWarnings("unchecked")
		ArrayList<Room> StructuredArray = (ArrayList<Room>) Rooms.clone();

		for (int i = 1; i < Rooms.size() - 1; i++) {

			Room Closest = Rooms.get(i);
			int ClosestValue = Closest.returnDistanceBetweenRooms(MainRoom);
			int Position = i;

			for (int b = i + 1; b < Rooms.size() - 1; b++) {

				Room FindRoom = Rooms.get(b);
				int TestingValue = MainRoom.returnDistanceBetweenRooms(FindRoom);

				if (TestingValue < ClosestValue && !FindRoom.equals(MainRoom)) {
					ClosestValue = TestingValue;
					Closest = FindRoom;
					Position = b;
				}

			}

			Collections.swap(StructuredArray, i, Position);

		}

		Rooms = StructuredArray;

	}

	public void TunnelPathing() {

		/*
		 * This method is dedicated to making paths between rooms. This connects all of the randomly generated rooms
		 * together so that a dungeon may be formed.
		 */

		ArrayChange();

		for (int RoomNum = 0; RoomNum < Rooms.size() - 1; RoomNum++) {
			System.out.print((1 + RoomNum) + " ");

			// This is currently set to simply path to the next generated room. Need to path to the next closest room.
			Room Original = Rooms.get(RoomNum);
			Room NextRoom = Rooms.get(RoomNum + 1);

			/*
			 * This loop is set to test every room in the ArrayList(Room) to see which one is the closest distance to
			 * the Original room(The current one thats trying to path to the closest room.)
			 * 
			 * This still needs a bunch of tweaking to get it to work properly. Running into a bunch of problems with
			 * Rooms being matched with illogical rooms. This appears to be a problem with the logic in this loop. It
			 * will be tweaked and fixed over time until it's properly done.
			 * 
			 * Rooms seem to be pathed to multiple times.
			 */

			int ClosestValue = Original.returnDistanceBetweenRooms(NextRoom);

			for (Room FindRoom : Rooms) {

				int TestingValue = Original.returnDistanceBetweenRooms(FindRoom);

				if (TestingValue < ClosestValue && !FindRoom.isConnected() && !FindRoom.hasParent()
						&& !FindRoom.equals(Original)) {

					ClosestValue = TestingValue;
					NextRoom = FindRoom;

				}

			}

			Original.SetConnectedtoMain();
			NextRoom.SetConnectedtoMain();
			NextRoom.setParent(Original);

			TunnelDigger.PathBetweenRooms(Original, NextRoom);
			DrawRoom(Original); DrawRoom(NextRoom);
		}

		System.out.println(".");

	}

	public void TranslatetoGrid() {

		// Properly set the rooms in the Grid to appear.
		// Set the walls and floors of each of the squares within a room.

		for (Room RoomTest : Rooms) {
			
			DrawRoom(RoomTest);

		}

		Nodes[(int) Rooms.get(0).CenterPoint().x][(int) Rooms.get(0).CenterPoint().y].setStart();

		Nodes[(int) Rooms.get(Rooms.size()-1).CenterPoint().x][(int) Rooms.get(Rooms.size()-1).CenterPoint().y].setStart();
	}

	public void DrawRoom(Room DrawRoom) {

		// Set the outside of the room as blocked
		for (int X = (int) DrawRoom.getRectangle().getX(); X < (DrawRoom.getRectangle().getX() + DrawRoom
				.getRectangle().getWidth()); X++) {
			for (int Y = (int) DrawRoom.getRectangle().getY(); Y < (DrawRoom.getRectangle().getY() + DrawRoom
					.getRectangle().getHeight()); Y++) {

				if (!Nodes[X][Y].isFloor()) {
					Nodes[X][Y].setBlocked();
				}

			}

		}

		// Set the inside of the rooms as floors

		for (int X = (int) DrawRoom.getRectangle().getX() + 1; X < (DrawRoom.getRectangle().getX()
				+ DrawRoom.getRectangle().getWidth() - 1); X++) {
			for (int Y = (int) DrawRoom.getRectangle().getY() + 1; Y < (DrawRoom.getRectangle().getY() + DrawRoom
					.getRectangle().getHeight()) - 1; Y++) {

				Nodes[X][Y].setFloor();

			}

		}

	}

}
