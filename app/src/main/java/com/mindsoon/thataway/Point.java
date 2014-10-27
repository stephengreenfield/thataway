package com.mindsoon.thataway;

public class Point extends android.graphics.Point{

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /* This function inputs width and height of a space, along with an angle in degrees.
    ** It converts the angle into radians, then calculates an x,y position at the periphery of
    ** the space -- kind of like squeezing a circle into a rectangular space.
    */
    void plotLocation(float t, double minX, double minY, double maxX, double maxY) {
        double tan = Math.tan( Math.toRadians( Math.abs(t%90) ) ),
                halfX = (minX + maxX) / 2,
                halfY = (minY + maxY) / 2,
                x = 0,
                y = 0,
                atanXoverY = Math.toDegrees( Math.atan( (maxX-minX) / (maxY-minY) ) ),
                atanYoverX = Math.toDegrees( Math.atan( (maxY-minY) / (maxX-minX) ) );

        if ( t == 0 || t == 360 )        {
            x = halfX;
            y = minY;
        } else if (t == 90 )  {
            x = minX;
            y = halfY;
        } else if ( t == 180 ) {
            x = halfX;
            y = maxY;
        } else if ( t == 270 ) {
            x = maxX;
            y = halfY;
        } else if ( t < 90 ) {
            if (t < atanXoverY) {
                x = halfX - halfY * tan;
                y = minY;
            } else {
                x = minX;
                y = halfY - halfX / tan;
            }
        } else if ( ( 90 < t ) && ( t < 180 ) ) {
            if ( t%90 < atanYoverX ) {
                x = minX;
                y = halfY + ( halfX * tan );
            } else {
                x = halfX - ( halfY / tan );
                y = maxY;
            }
        } else if ( ( 180 < t ) && ( t < 270 ) ) {
            if (t%90 < atanXoverY ) {
                x = halfX + (halfY * tan);
                y = maxY;
            } else {
                x = maxX;
                y = halfY + (halfX / tan);
            }
        } else if ( t > 270 ) {
            if (t%90 < atanYoverX) {
                x = maxX;
                y = halfY - (halfX * tan);
            } else {
                x = halfX + (halfY / tan);
                y = minY;
            }
        }

        if ( x < minX ) x = minX;
        else if ( x > maxX ) x = maxX;
        if ( y < minY ) y = minY;
        else if ( y > maxY ) y = maxY;

        this.x = (int) x;
        this.y = (int) y;
    }
}