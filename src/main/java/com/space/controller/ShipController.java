package com.space.controller;

import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.service.ShipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/rest/ships")
public class ShipController {

    private final ShipService shipService;

    @Autowired
    public ShipController(ShipService shipService) {
        this.shipService = shipService;
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Ship> getShip(@PathVariable(name = "id") String id) {
        Long shipId = 0L;
        try {
            shipId = Long.valueOf(id);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (shipId <= 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Ship ship = shipService.getById(shipId);

        if (ship == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(ship, HttpStatus.OK);
    }

    @PostMapping(value = "")
    public ResponseEntity<Ship> saveShip(@RequestBody Ship ship) {
        if (ship.getName() == null || ship.getName().length() > 50 || ship.getName().length() == 0 ||
                ship.getPlanet() == null || ship.getPlanet().length() > 50 || ship.getPlanet().length() == 0 ||
                ship.getShipType() == null || ship.getProdDate() == null || ship.getProdDate().getTime() < 0 ||
                ship.getSpeed() == null || ship.getSpeed() < 0.01 || ship.getSpeed() > 0.99 ||
                ship.getCrewSize() == null || ship.getCrewSize() < 1 || ship.getCrewSize() > 9999) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(ship.getProdDate().getTime());

        if (calendar.get(Calendar.YEAR) < 2800 || calendar.get(Calendar.YEAR) > 3019) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (ship.getUsed() == null) {
            ship.setUsed(false);
        }

        ship.updateRating();

        shipService.save(ship);

        return new ResponseEntity<>(ship, HttpStatus.OK);
    }

    @GetMapping(value = "")
    public ResponseEntity<List<Ship>> getShips(HttpServletRequest request) {
        List<Ship> ships = selectShipsWithParameters(request);
        ShipOrder order = null;
        try {
            order = ShipOrder.valueOf(request.getParameter("order"));
        } catch (Exception ignored) {}
        Integer pageNumber = 0;
        try {
            pageNumber = Integer.valueOf(request.getParameter("pageNumber"));
        } catch (Exception ignored) {}
        Integer pageSize = 3;
        try {
            pageSize = Integer.valueOf(request.getParameter("pageSize"));
        } catch (Exception ignored) {}

        List<Ship> outputShips = new ArrayList<>();

        if (order != null) {
            final ShipOrder shipOrder = order;
            Collections.sort(ships,new Comparator<Ship>(){
                public int compare(Ship s1,Ship s2){
                    int result = 0;
                    switch (shipOrder) {
                        case ID:
                            result = s1.getId().compareTo(s2.getId());
                            break;
                        case DATE:
                            result = Long.compare(s1.getProdDate().getTime(), s2.getProdDate().getTime());
                            break;
                        case SPEED:
                            result = Double.compare(s1.getSpeed(), s2.getSpeed());
                            break;
                        case RATING:
                            result = Double.compare(s1.getRating(), s2.getRating());
                            break;
                    }
                    return result;
                }});
        }

        int outputPagesCount = (int) Math.ceil(ships.size() * 1.0 / pageSize);
        if (outputPagesCount > pageNumber) {
            for (int i = pageNumber * pageSize; i < (pageNumber + 1) * pageSize; i++) {
                if (i >= ships.size()) {
                    break;
                }
                outputShips.add(ships.get(i));
            }
        }

        return new ResponseEntity<>(outputShips, HttpStatus.OK);
    }

    @GetMapping(value = "/count")
    public ResponseEntity<Integer> getShipsCount(HttpServletRequest request) {
        List<Ship> outputShips = selectShipsWithParameters(request);
        return new ResponseEntity<>(outputShips.size(), HttpStatus.OK);
    }

    @PostMapping(value = "/{id}")
    public ResponseEntity<Ship> updateShip(@RequestBody Ship ship, @PathVariable(name = "id") String id) {
        Long shipId = 0L;
        try {
            shipId = Long.valueOf(id);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (shipId <= 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Ship oldShip = shipService.getById(shipId);

        if (oldShip == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean isUpdated = false;
        if (ship.getName() != null) {
            if (ship.getName().length() <= 50 && ship.getName().length() > 0) {
                oldShip.setName(ship.getName());
                isUpdated = true;
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        if (ship.getPlanet() != null) {
            if (ship.getPlanet().length() <= 50 && ship.getPlanet().length() > 0) {
                oldShip.setPlanet(ship.getPlanet());
                isUpdated = true;
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        if (ship.getShipType() != null) {
            oldShip.setShipType(ship.getShipType());
            isUpdated = true;
        }

        if (ship.getProdDate() != null) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(ship.getProdDate().getTime());
            if (calendar.get(Calendar.YEAR) >= 2800 && calendar.get(Calendar.YEAR) <= 3019) {
                oldShip.setProdDate(new Date(ship.getProdDate().getTime()));
                isUpdated = true;
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        if (ship.getUsed() != null) {
            oldShip.setUsed(ship.getUsed());
            isUpdated = true;
        }

        if (ship.getSpeed() != null) {
            if (ship.getSpeed() >= 0.01 && ship.getSpeed() <= 0.99) {
                oldShip.setSpeed(ship.getSpeed());
                isUpdated = true;
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        if (ship.getCrewSize() != null) {
            if (ship.getCrewSize() >=1 && ship.getCrewSize() <= 9999) {
                oldShip.setCrewSize(ship.getCrewSize());
                isUpdated = true;
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        if (isUpdated) {
            oldShip.updateRating();
            shipService.update(oldShip, shipId);
        }
        return new ResponseEntity<>(oldShip, HttpStatus.OK);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity deleteShip(@PathVariable(name = "id") String id) {
        Long shipId = 0L;
        try {
            shipId = Long.valueOf(id);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        if (shipId <= 0) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        Ship ship = shipService.getById(shipId);

        if (ship == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        shipService.delete(shipId);

        return new ResponseEntity(HttpStatus.OK);
    }

    public List<Ship> selectShipsWithParameters(HttpServletRequest request) {
        String name = request.getParameter("name");
        String planet = request.getParameter("planet");
        ShipType shipType = null;
        try {
            shipType = ShipType.valueOf(request.getParameter("shipType"));
        } catch (Exception ignored) {}
        Long after = null;
        try {
            after = Long.valueOf(request.getParameter("after"));
        } catch (Exception ignored) {}
        Long before = null;
        try {
            before = Long.valueOf(request.getParameter("before"));
        } catch (Exception ignored) {}
        Boolean isUsed = null;
        if ("true".equals(request.getParameter("isUsed"))) {
            isUsed = true;
        } else if ("false".equals(request.getParameter("isUsed"))) {
            isUsed = false;
        }
        Double minSpeed = null;
        try {
            minSpeed = Double.valueOf(request.getParameter("minSpeed"));
        } catch (Exception ignored) {}
        Double maxSpeed = null;
        try {
            maxSpeed = Double.valueOf(request.getParameter("maxSpeed"));
        } catch (Exception ignored) {}
        Integer minCrewSize = null;
        try {
            minCrewSize = Integer.valueOf(request.getParameter("minCrewSize"));
        } catch (Exception ignored) {}
        Integer maxCrewSize = null;
        try {
            maxCrewSize = Integer.valueOf(request.getParameter("maxCrewSize"));
        } catch (Exception ignored) {}
        Double minRating = null;
        try {
            minRating = Double.valueOf(request.getParameter("minRating"));
        } catch (Exception ignored) {}
        Double maxRating = null;
        try {
            maxRating = Double.valueOf(request.getParameter("maxRating"));
        } catch (Exception ignored) {}

        List<Ship> ships = shipService.getAll();
        List<Ship> outputShips = new ArrayList<>();

        for (Ship ship : ships) {
            if (name != null && !ship.getName().contains(name)) {
                continue;
            }

            if (planet != null && !ship.getPlanet().contains(planet)) {
                continue;
            }

            if (shipType != null && !ship.getShipType().equals(shipType)) {
                continue;
            }

            if (after != null && ship.getProdDate().getTime() < after) {
                continue;
            }

            if (before != null && ship.getProdDate().getTime() > before) {
                continue;
            }

            if (isUsed != null && ship.getUsed() != isUsed) {
                continue;
            }

            if (minSpeed != null && ship.getSpeed() < minSpeed) {
                continue;
            }

            if (maxSpeed != null && ship.getSpeed() > maxSpeed) {
                continue;
            }

            if (minCrewSize != null && ship.getCrewSize() < minCrewSize) {
                continue;
            }

            if (maxCrewSize != null && ship.getCrewSize() > maxCrewSize) {
                continue;
            }

            if (minRating != null && ship.getRating() < minRating) {
                continue;
            }

            if (maxRating != null && ship.getRating() > maxRating) {
                continue;
            }

            outputShips.add(ship);
        }

        return outputShips;
    }

}
