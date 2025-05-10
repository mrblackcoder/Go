/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package game.go.model;

/**
 *
 * @author METABOY
 */
public class Test {
    public static void main(String[] args) {
    Board b = new Board(5);
    // köşe iki siyah taşla kapat
    b.placeStone(new Point(0,1), Stone.BLACK);
    b.placeStone(new Point(1,0), Stone.BLACK);
    int area = b.areaControlledBy(Stone.BLACK);
    System.out.println("Controlled corners by BLACK: " + area);
}
}
