package com.example.swachhbot.simulation

import com.example.swachhbot.model.*

object DemoMapFactory {
    fun createDemoHouse(): HouseMap {
        val livingRoom = Room(
            id = "f47ac10b-58cc-4372-a567-0e02b2c3d479", name = "Living Room", x = 0f, y = 0f, width = 600f, height = 600f,
            furniture = listOf(
                Furniture("F1", FurnitureType.SOFA, 300f, 100f, 200f, 80f, 0f),
                Furniture("F2", FurnitureType.TABLE, 300f, 220f, 120f, 60f, 0f),
                Furniture("F3", FurnitureType.TABLE, 100f, 500f, 150f, 40f, 45f) // TV Unit
            )
        )

        val kitchen = Room(
            id = "550e8400-e29b-41d4-a716-446655440000", name = "Kitchen", x = 600f, y = 0f, width = 400f, height = 600f,
            furniture = listOf(
                Furniture("F4", FurnitureType.REFRIGERATOR, 900f, 100f, 80f, 80f),
                Furniture("F5", FurnitureType.TABLE, 800f, 350f, 140f, 140f),
                Furniture("F6", FurnitureType.CHAIR, 700f, 350f, 40f, 40f)
            )
        )

        val bedroom = Room(
            id = "6ba7b810-9dad-11d1-80b4-00c04fd430c8", name = "Bedroom", x = 0f, y = 600f, width = 600f, height = 400f,
            furniture = listOf(
                Furniture("F7", FurnitureType.BED, 150f, 800f, 180f, 220f, 90f)
            )
        )

        val bathroom = Room(
            id = "6ba7b811-9dad-11d1-80b4-00c04fd430c8", name = "Bathroom", x = 600f, y = 600f, width = 400f, height = 400f
        )

        // Add walls with gaps for "doors"
        val walls = listOf(
            // Outer boundaries
            Wall(0f, 0f, 1000f, 0f),
            Wall(1000f, 0f, 1000f, 1000f),
            Wall(1000f, 1000f, 0f, 1000f),
            Wall(0f, 1000f, 0f, 0f),
            
            // Vertical divider (Living Room / Kitchen) with door
            Wall(600f, 0f, 600f, 200f),
            Wall(600f, 350f, 600f, 600f),
            
            // Horizontal divider (Living Room / Bedroom) with door
            Wall(0f, 600f, 200f, 600f),
            Wall(350f, 600f, 600f, 600f),
            
            // Bedroom / Bathroom divider
            Wall(600f, 600f, 600f, 1000f),
            
            // Kitchen / Bathroom divider with door
            Wall(600f, 600f, 700f, 600f),
            Wall(850f, 600f, 1000f, 600f)
        )

        return HouseMap(
            width = 1000f,
            height = 1000f,
            rooms = listOf(livingRoom, kitchen, bedroom, bathroom),
            walls = walls
        )
    }
}
