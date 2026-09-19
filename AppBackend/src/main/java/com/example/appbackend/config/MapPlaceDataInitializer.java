package com.example.appbackend.config;

import com.example.appbackend.entity.MapPlace;
import com.example.appbackend.entity.MapPlaceFence;
import com.example.appbackend.repository.MapPlaceFenceRepository;
import com.example.appbackend.repository.MapPlaceRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MapPlaceDataInitializer implements ApplicationRunner {

    private static final String CAMPUS_BOUNDARY_GEOJSON = """
            {"type":"Polygon","coordinates":[[
              [104.1497524,30.6712365],[104.1483581,30.6708613],[104.1479269,30.6707668],[104.1468099,30.6704367],
              [104.1444018,30.6697248],[104.1434026,30.6692605],[104.1431012,30.6690977],[104.1428205,30.6691994],
              [104.1426366,30.6690465],[104.1395855,30.6712317],[104.1385948,30.6719413],[104.1385292,30.6719921],
              [104.1383506,30.6725576],[104.1383225,30.6726202],[104.1382723,30.6727552],[104.1382499,30.6728114],
              [104.1381859,30.672973],[104.1381354,30.6731005],[104.1424182,30.6774292],[104.1425233,30.6775558],
              [104.1454102,30.6804199],[104.145886,30.6800654],[104.146201,30.6800539],[104.1462495,30.6801091],
              [104.146332,30.6803275],[104.1464462,30.6802682],[104.1467793,30.6800269],[104.1474455,30.6795438],
              [104.147712,30.6798479],[104.1481377,30.6801686],[104.1488488,30.6806212],[104.1497199,30.6808935],
              [104.1505605,30.6810514],[104.151462,30.6810305],[104.1519378,30.6809558],[104.1521365,30.6809247],
              [104.1524498,30.6808672],[104.1525833,30.6809747],[104.1526433,30.6809457],[104.1527335,30.680895],
              [104.1528295,30.6808449],[104.1528754,30.6808188],[104.1529071,30.6806277],[104.153337,30.6802471],
              [104.1539717,30.6797424],[104.1542549,30.6793532],[104.1543547,30.6789144],[104.1556349,30.6789008],
              [104.1556675,30.6774436],[104.155676,30.6763798],[104.1556931,30.6752051],[104.154926,30.6751596],
              [104.1541268,30.6752015],[104.153669,30.6744558],[104.1532486,30.6740562],[104.1528453,30.6737961],
              [104.1532781,30.6720465],[104.151612,30.6717162],[104.1497524,30.6712365]
            ]]}
            """;

    private final MapPlaceRepository mapPlaceRepository;
    private final MapPlaceFenceRepository fenceRepository;

    public MapPlaceDataInitializer(
            MapPlaceRepository mapPlaceRepository,
            MapPlaceFenceRepository fenceRepository
    ) {
        this.mapPlaceRepository = mapPlaceRepository;
        this.fenceRepository = fenceRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<MapPlace> existingPlaces = mapPlaceRepository.findAll();
        Set<String> existingNames = existingPlaces.stream()
                .map(MapPlace::getName)
                .collect(Collectors.toSet());

        Set<String> legacyNames = Set.of(
                "学一食堂", "学二食堂", "学三食堂", "蜜雪冰城",
                "东区运动场", "篮球场", "田径场", "排球场",
                "明德楼", "崇德楼", "图书馆", "综合服务楼",
                "机械工程学院", "经济管理学院", "中心景观湖",
                "明志行政楼", "朝阳校医院"
        );
        List<MapPlace> legacyPlaces = existingPlaces.stream()
                .filter(place -> legacyNames.contains(place.getName())
                        && String.valueOf(place.getLocationDesc()).contains("朝阳校区"))
                .toList();
        mapPlaceRepository.deleteAll(legacyPlaces);

        Set<String> obsoleteCampusNames = Set.of(
                "操场看台", "生活区10幢",
                "北翼楼", "南翼楼", "地质灾害防治与地质环境保护国家重点实验室",
                "环境与土木工程学院", "地理与规划学院", "网络安全学院", "商学院",
                "主入口"
        );
        List<MapPlace> obsoleteCampusPlaces = existingPlaces.stream()
                .filter(place -> obsoleteCampusNames.contains(place.getName())
                        && String.valueOf(place.getLocationDesc()).contains("成都理工大学成都校区"))
                .toList();
        mapPlaceRepository.deleteAll(obsoleteCampusPlaces);
        existingNames.removeAll(obsoleteCampusNames);

        existingPlaces.stream()
                .filter(place -> "松林园".equals(place.getName())
                        && String.valueOf(place.getLocationDesc()).contains("成都理工大学成都校区"))
                .forEach(place -> {
                    place.setLongitude(new BigDecimal("104.1527900"));
                    place.setLatitude(new BigDecimal("30.6728170"));
                    mapPlaceRepository.save(place);
                });

        List<MapPlace> seeds = List.of(
                // 食堂
                place("CANTEEN", "CANTEEN", "芙蓉食堂", "成都理工大学成都校区校园食堂。", "成都理工大学成都校区", "104.1403674", "30.6745194", 1),
                place("CANTEEN", "CANTEEN", "银杏餐厅", "成都理工大学成都校区校园食堂。", "成都理工大学成都校区", "104.1435442", "30.6701312", 2),
                place("CANTEEN", "CANTEEN", "珙桐园食堂", "成都理工大学成都校区校园食堂。", "成都理工大学成都校区", "104.1494160", "30.6727072", 3),
                place("CANTEEN", "CANTEEN", "香樟食堂", "成都理工大学成都校区校园食堂。", "成都理工大学成都校区", "104.1548536", "30.6768845", 4),
                // 宿舍
                place("DORMITORY", "DORMITORY", "香樟园", "成都理工大学成都校区学生宿舍区。", "成都理工大学成都校区", "104.1550260", "30.6773883", 10),
                place("DORMITORY", "DORMITORY", "松林园", "成都理工大学成都校区学生宿舍区。", "成都理工大学成都校区", "104.1527900", "30.6728170", 11),
                place("DORMITORY", "DORMITORY", "珙桐园", "成都理工大学成都校区学生宿舍区。", "成都理工大学成都校区", "104.1488639", "30.6717419", 12),
                place("DORMITORY", "DORMITORY", "银杏园", "成都理工大学成都校区学生宿舍区。", "成都理工大学成都校区", "104.1454538", "30.6705182", 13),
                place("DORMITORY", "DORMITORY", "榕树园", "成都理工大学成都校区学生宿舍区。", "成都理工大学成都校区", "104.1430067", "30.6715771", 14),
                place("DORMITORY", "DORMITORY", "芙蓉园", "成都理工大学成都校区学生宿舍区。", "成都理工大学成都校区", "104.1431099", "30.6764386", 15),
                // 教学/科研
                place("TEACHING", "TEACHING_BUILDING", "第一教学楼", "成都理工大学成都校区教学与公共教室。", "成都理工大学成都校区", "104.1426518", "30.6755090", 20),
                place("TEACHING", "TEACHING_BUILDING", "第二教学楼", "成都理工大学成都校区教学与公共教室。", "成都理工大学成都校区", "104.1444130", "30.6760736", 21),
                place("TEACHING", "TEACHING_BUILDING", "第三教学楼", "成都理工大学成都校区教学与公共教室。", "成都理工大学成都校区", "104.1434664", "30.6761670", 22),
                place("TEACHING", "TEACHING_BUILDING", "第四教学楼", "成都理工大学成都校区教学与公共教室。", "成都理工大学成都校区", "104.1431951", "30.6759920", 23),
                place("TEACHING", "TEACHING_BUILDING", "第六教学楼", "成都理工大学成都校区教学与公共教室。", "成都理工大学成都校区", "104.1450571", "30.6750289", 24),
                place("TEACHING", "TEACHING_BUILDING", "第七教学楼", "成都理工大学成都校区教学与公共教室。", "成都理工大学成都校区", "104.1431138", "30.6766559", 25),
                place("TEACHING", "TEACHING_BUILDING", "第八教学楼", "成都理工大学成都校区教学与公共教室。", "成都理工大学成都校区", "104.1459870", "30.6737942", 26),
                place("TEACHING", "TEACHING_BUILDING", "综合楼", "成都理工大学成都校区综合教学楼。", "成都理工大学成都校区", "104.1443727", "30.6737472", 29),
                place("TEACHING", "TEACHING_BUILDING", "东区第一教学楼", "成都理工大学成都校区东区教学与公共教室。", "成都理工大学成都校区", "104.1503902", "30.6781852", 35),
                place("TEACHING", "TEACHING_BUILDING", "东区第二教学楼", "成都理工大学成都校区东区教学与公共教室。", "成都理工大学成都校区", "104.1518986", "30.6774129", 36),
                place("TEACHING", "TEACHING_BUILDING", "综合实验楼", "成都理工大学成都校区综合实验楼。", "成都理工大学成都校区", "104.1527547", "30.6746296", 37),
                // 运动场
                place("SPORTS", "SPORTS_GROUND", "东一运动场", "成都理工大学成都校区东区运动场。", "成都理工大学成都校区", "104.1534713", "30.6758057", 40),
                place("SPORTS", "SPORTS_GROUND", "篮球场", "成都理工大学成都校区篮球场。", "成都理工大学成都校区", "104.1516744", "30.6726556", 41),
                place("SPORTS", "SPORTS_GROUND", "网球场", "成都理工大学成都校区网球场。", "成都理工大学成都校区", "104.1417615", "30.6707902", 42),
                place("SPORTS", "SPORTS_GROUND", "西区操场", "成都理工大学成都校区西区操场。", "成都理工大学成都校区", "104.1406179", "30.6718178", 43),
                place("SPORTS", "SPORTS_GROUND", "游泳场", "成都理工大学成都校区游泳场。", "成都理工大学成都校区", "104.1395615", "30.6719117", 44),
                place("SPORTS", "SPORTS_GROUND", "散打馆", "成都理工大学成都校区散打馆。", "成都理工大学成都校区", "104.1399560", "30.6713561", 45),
                place("SPORTS", "SPORTS_GROUND", "成都理工大学体育馆", "成都理工大学成都校区体育馆。", "成都理工大学成都校区", "104.1400596", "30.6727341", 46),
                // 其他
                place("OTHER", "HOSPITAL", "校医院", "成都理工大学成都校区医疗服务点。", "成都理工大学成都校区", "104.1427438", "30.6769351", 50),
                place("OTHER", "MUSEUM", "成都理工大学博物馆", "成都理工大学博物馆。", "成都理工大学成都校区", "104.1439950", "30.6784734", 51),
                place("OTHER", "LANDSCAPE", "孔子雕塑", "成都理工大学成都校区景观节点。", "成都理工大学成都校区", "104.1415793", "30.6736473", 52),
                place("OTHER", "LANDSCAPE", "三角草坪雕塑", "成都理工大学成都校区景观节点。", "成都理工大学成都校区", "104.1401425", "30.6732854", 53),
                place("OTHER", "LANDSCAPE", "岭南阁", "成都理工大学成都校区休闲景观。", "成都理工大学成都校区", "104.1504185", "30.6772952", 54),
                // 校园围栏
                place("OTHER", "CAMPUS_BOUNDARY", "成都理工大学成都校区边界", "成都理工大学成都校区地图围栏。", "成都理工大学成都校区", "104.1468670", "30.6748201", 0)
        );

        seeds.stream()
                .filter(place -> !existingNames.contains(place.getName()))
                .forEach(mapPlaceRepository::save);

        MapPlace campusBoundary = mapPlaceRepository.findBySceneTypeOrderBySortOrderAscIdAsc("OTHER")
                .stream()
                .filter(place -> "CAMPUS_BOUNDARY".equals(place.getPlaceType()))
                .findFirst()
                .orElse(null);
        if (campusBoundary != null) {
            MapPlaceFence fence = fenceRepository.findByPlaceId(campusBoundary.getId())
                    .orElseGet(MapPlaceFence::new);
            fence.setPlaceId(campusBoundary.getId());
            fence.setGeometryType("POLYGON");
            fence.setGeometryData(CAMPUS_BOUNDARY_GEOJSON.trim());
            fenceRepository.save(fence);
        }
    }

    private MapPlace place(
            String sceneType,
            String placeType,
            String name,
            String description,
            String locationDesc,
            String longitude,
            String latitude,
            int sortOrder
    ) {
        MapPlace place = new MapPlace();
        place.setSceneType(sceneType);
        place.setPlaceType(placeType);
        place.setName(name);
        place.setDescription(description);
        place.setLocationDesc(locationDesc);
        place.setLongitude(new BigDecimal(longitude));
        place.setLatitude(new BigDecimal(latitude));
        place.setStatus("ENABLED");
        place.setMapVisible(true);
        place.setSortOrder(sortOrder);
        return place;
    }
}
