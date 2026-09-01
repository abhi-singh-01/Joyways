-- Demo package seed data for JOYWAY MVP
-- All data is clearly marked as DEMO for development/testing purposes

INSERT INTO packages (
    title, slug, destination, short_description, full_description,
    departure_city, pickup_points, start_date, end_date, duration,
    price_per_person, total_seats, available_seats,
    hotel_name, hotel_details, vehicle_type, itinerary, inclusions, exclusions,
    cancellation_policy, status
) VALUES
(
    'Nainital Weekend Escape (DEMO)',
    'nainital-weekend-escape-demo',
    'Nainital',
    'Escape to the serene hills of Nainital with a curated group trip from Delhi.',
    'Experience the charm of Nainital with our carefully planned weekend getaway. This DEMO package includes comfortable transport, hotel stay, and guided sightseeing. Perfect for first-time hill travellers.',
    'Delhi',
    'Connaught Place, Karol Bagh, Laxmi Nagar',
    CURRENT_DATE + INTERVAL '14 days',
    CURRENT_DATE + INTERVAL '16 days',
    '3 Days / 2 Nights',
    4999.00,
    20,
    18,
    'Demo Hill View Hotel (DEMO)',
    'Standard double sharing rooms with basic amenities. Demo property for MVP testing.',
    'Tempo Traveller (12+1)',
    'Day 1: Delhi to Nainital, check-in, Mall Road evening walk
Day 2: Naini Lake boating, Snow View Point, local market
Day 3: Check-out, return to Delhi',
    'AC Tempo Traveller from Delhi
2 nights hotel stay (twin sharing)
Breakfast on Day 2 & 3
Basic sightseeing as per itinerary
Trip coordinator',
    'Lunch and dinner
Personal expenses
Adventure activities
Travel insurance',
    'Cancellation 7+ days before departure: 80% refund. 3-7 days: 50% refund. Less than 3 days: no refund. (DEMO policy)',
    'PUBLISHED'
),
(
    'Mussoorie Hill Retreat (DEMO)',
    'mussoorie-hill-retreat-demo',
    'Mussoorie',
    'Discover the Queen of Hills with a hassle-free group trip from Delhi.',
    'Join fellow travellers on a scenic journey to Mussoorie. This DEMO package showcases our complete travel experience with transport, stay, and curated activities.',
    'Delhi',
    'Connaught Place, Rohini, Noida Sector 18',
    CURRENT_DATE + INTERVAL '21 days',
    CURRENT_DATE + INTERVAL '23 days',
    '3 Days / 2 Nights',
    5499.00,
    18,
    15,
    'Demo Cloud Nine Resort (DEMO)',
    'Comfortable rooms with mountain views. Demo property for MVP testing.',
    'Tempo Traveller (12+1)',
    'Day 1: Delhi to Mussoorie, check-in, Camel Back Road walk
Day 2: Kempty Falls, Gun Hill, Mall Road
Day 3: Check-out, return to Delhi',
    'AC Tempo Traveller from Delhi
2 nights hotel stay (twin sharing)
Breakfast on Day 2 & 3
Basic sightseeing as per itinerary
Trip coordinator',
    'Lunch and dinner
Personal expenses
Cable car rides
Travel insurance',
    'Cancellation 7+ days before departure: 80% refund. 3-7 days: 50% refund. Less than 3 days: no refund. (DEMO policy)',
    'PUBLISHED'
),
(
    'Rishikesh Adventure & Yoga (DEMO)',
    'rishikesh-adventure-yoga-demo',
    'Rishikesh',
    'Experience spirituality and adventure in the Yoga Capital of the World.',
    'A perfect blend of adventure and tranquillity in Rishikesh. This DEMO package includes river-side stay, basic rafting experience, and Ganga Aarti visit.',
    'Delhi',
    'Connaught Place, Dwarka, Gurgaon Cyber Hub',
    CURRENT_DATE + INTERVAL '10 days',
    CURRENT_DATE + INTERVAL '12 days',
    '3 Days / 2 Nights',
    5999.00,
    16,
    12,
    'Demo Ganga View Camp (DEMO)',
    'Riverside camping with attached washrooms. Demo property for MVP testing.',
    'Tempo Traveller (12+1)',
    'Day 1: Delhi to Rishikesh, check-in, Ganga Aarti at Triveni Ghat
Day 2: Morning yoga session, white water rafting (basic stretch)
Day 3: Check-out, return to Delhi',
    'AC Tempo Traveller from Delhi
2 nights camp/resort stay
Breakfast on Day 2 & 3
Basic rafting (16km stretch)
Trip coordinator',
    'Lunch and dinner
Personal expenses
Advanced adventure activities
Travel insurance',
    'Cancellation 7+ days before departure: 80% refund. 3-7 days: 50% refund. Less than 3 days: no refund. (DEMO policy)',
    'PUBLISHED'
),
(
    'Haridwar Spiritual Journey (DEMO)',
    'haridwar-spiritual-journey-demo',
    'Haridwar',
    'A soulful group pilgrimage to the gateway of the Gods.',
    'Experience the spiritual essence of Haridwar with our curated group trip. This DEMO package includes temple visits, Ganga Aarti, and comfortable stay.',
    'Delhi',
    'Connaught Place, Anand Vihar, Ghaziabad',
    CURRENT_DATE + INTERVAL '18 days',
    CURRENT_DATE + INTERVAL '19 days',
    '2 Days / 1 Night',
    3499.00,
    22,
    20,
    'Demo Holy Ganges Hotel (DEMO)',
    'Clean and comfortable rooms near Har Ki Pauri. Demo property for MVP testing.',
    'Tempo Traveller (12+1)',
    'Day 1: Delhi to Haridwar, check-in, Har Ki Pauri, evening Ganga Aarti
Day 2: Mansa Devi Temple, Chandi Devi Temple, return to Delhi',
    'AC Tempo Traveller from Delhi
1 night hotel stay (twin sharing)
Breakfast on Day 2
Basic sightseeing as per itinerary
Trip coordinator',
    'Lunch and dinner
Personal expenses
Cable car tickets
Travel insurance',
    'Cancellation 7+ days before departure: 80% refund. 3-7 days: 50% refund. Less than 3 days: no refund. (DEMO policy)',
    'PUBLISHED'
),
(
    'Dalhousie Snow Escape (DEMO)',
    'dalhousie-snow-escape-demo',
    'Dalhousie',
    'Explore the colonial charm of Dalhousie with snow-capped mountain views.',
    'Journey to the picturesque hill station of Dalhousie. This DEMO package offers a longer getaway with scenic drives, colonial architecture, and mountain vistas.',
    'Delhi',
    'Connaught Place, Punjabi Bagh, Faridabad',
    CURRENT_DATE + INTERVAL '28 days',
    CURRENT_DATE + INTERVAL '31 days',
    '4 Days / 3 Nights',
    8999.00,
    15,
    14,
    'Demo Pine Valley Resort (DEMO)',
    'Heritage-style rooms with valley views. Demo property for MVP testing.',
    'Tempo Traveller (12+1)',
    'Day 1: Delhi to Dalhousie (overnight journey), check-in
Day 2: Khajjiar (Mini Switzerland), Panchpula waterfalls
Day 3: Kalatop Wildlife Sanctuary, St. John Church
Day 4: Check-out, return to Delhi',
    'AC Tempo Traveller from Delhi
3 nights hotel stay (twin sharing)
Breakfast on Day 2, 3 & 4
Basic sightseeing as per itinerary
Trip coordinator',
    'Lunch and dinner
Personal expenses
Horse riding at Khajjiar
Travel insurance',
    'Cancellation 7+ days before departure: 80% refund. 3-7 days: 50% refund. Less than 3 days: no refund. (DEMO policy)',
    'PUBLISHED'
);

-- Package photos are uploaded via admin to Azure Blob Storage (private_storage_key only).
-- Upload images in admin after seeding demo packages.
