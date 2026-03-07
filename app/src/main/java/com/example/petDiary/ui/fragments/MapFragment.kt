package com.example.petDiary.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.petDiary.R
import com.example.petDiary.domain.model.PlacemarkData
import com.example.petDiary.ui.viewmodel.MapViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError

class MapFragment : Fragment(), UserLocationObjectListener {

    private lateinit var mapView: MapView
    private var userLocationLayer: UserLocationLayer? = null
    private var searchManager: SearchManager? = null
    private var mapObjects: MapObjectCollection? = null

    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipVetClinics: Chip
    private lateinit var chipPetShops: Chip
    private lateinit var btnSearch: MaterialButton

    private lateinit var viewModel: MapViewModel
    private var currentSearchSession: Session? = null
    private val placemarks = mutableListOf<PlacemarkMapObject>()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            enableUserLocation()
        } else {
            Snackbar.make(requireView(), "Для определения местоположения необходимо разрешение", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        } catch (e: Exception) {
            Log.e("MapFragment", "Ошибка инициализации поиска карты", e)
        }
        viewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]
        initViews(view)
        setupMap()
        setupClickListeners()
        setupObservers()
        checkLocationPermission()
    }

    private fun initViews(view: View) {
        mapView = view.findViewById(R.id.mapView)
        fabMyLocation = view.findViewById(R.id.fabMyLocation)
        chipGroup = view.findViewById(R.id.chipGroup)
        chipVetClinics = view.findViewById(R.id.chipVetClinics)
        chipPetShops = view.findViewById(R.id.chipPetShops)
        btnSearch = view.findViewById(R.id.btnSearch)
    }

    private fun setupMap() {
        try {
            mapObjects = mapView.map.mapObjects.addCollection()
            showCity(Point(51.660781, 39.200296), 12.0f)
        } catch (e: Exception) {
            Log.e("MapFragment", "Ошибка настройки карты", e)
            if (view != null) Snackbar.make(requireView(), "Ошибка загрузки карты", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        fabMyLocation.setOnClickListener {
            moveToUserLocation()
        }

        btnSearch.setOnClickListener {
            performSearch()
        }
    }

    private fun setupObservers() {
        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            // Location updates handled in onObjectAdded/onObjectUpdated
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            updateSearchResults(results)
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableUserLocation()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun enableUserLocation() {
        try {
            val mapKit = MapKitFactory.getInstance()
            userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
            userLocationLayer?.isVisible = true
            userLocationLayer?.setObjectListener(this)
        } catch (e: Exception) {
            Log.e("MapFragment", "Ошибка включения геолокации", e)
            e.printStackTrace()
        }
    }

    private fun moveToUserLocation() {
        val location = viewModel.userLocation.value
        if (location != null) {
            mapView.map.move(
                CameraPosition(location, 15.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1.0f),
                null
            )
        } else {
            val cameraPosition = userLocationLayer?.cameraPosition()
            if (cameraPosition != null) {
                mapView.map.move(
                    CameraPosition(cameraPosition.target, 15.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1.0f),
                    null
                )
            } else {
                Snackbar.make(requireView(), "Определение местоположения...", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun performSearch() {
        if (searchManager == null) {
            Snackbar.make(requireView(), "Поиск временно недоступен", Snackbar.LENGTH_LONG).show()
            return
        }

        clearPlacemarks()

        val searchPoint = viewModel.userLocation.value ?: Point(51.660781, 39.200296)
        val searchQueries = mutableListOf<String>()

        if (chipVetClinics.isChecked) {
            searchQueries.add("ветеринарная клиника")
        }

        if (chipPetShops.isChecked) {
            searchQueries.add("зоомагазин")
        }

        if (searchQueries.isEmpty()) {
            Snackbar.make(requireView(), "Выберите категорию для поиска", Snackbar.LENGTH_SHORT).show()
            return
        }

        searchQueries.forEach { query ->
            searchNearby(query, searchPoint)
        }

        Snackbar.make(requireView(), "Поиск...", Snackbar.LENGTH_SHORT).show()
    }

    private fun searchNearby(query: String, point: Point) {
        try {
            val searchOptions = SearchOptions().apply {
                searchTypes = SearchType.BIZ.value
                resultPageSize = 20
            }

            currentSearchSession = searchManager!!.submit(
                query,
                com.yandex.mapkit.map.VisibleRegionUtils.toPolygon(mapView.map.visibleRegion),
                searchOptions,
                object : Session.SearchListener {
                    override fun onSearchResponse(response: Response) {
                        handleSearchResponse(response, query)
                    }

                    override fun onSearchError(error: Error) {
                        handleSearchError(error)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("MapFragment", "Ошибка поиска", e)
            Snackbar.make(requireView(), "Ошибка поиска", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun handleSearchResponse(response: Response, query: String) {
        val isVetClinic = query.contains("ветеринар")
        val results = mutableListOf<PlacemarkData>()

        response.collection.children.forEach { item ->
            val point = item.obj?.geometry?.firstOrNull()?.point
            val name = item.obj?.name ?: "Без названия"
            val address = item.obj?.descriptionText ?: ""

            point?.let {
                val placemarkData = PlacemarkData(
                    name = name,
                    address = address,
                    isVetClinic = isVetClinic,
                    workingHours = null,
                    rating = null,
                    ratingsCount = null,
                    phones = emptyList(),
                    website = null,
                    point = it
                )
                results.add(placemarkData)
                addPlacemark(it, placemarkData)
            }
        }

        viewModel.setSearchResults(results)

        if (response.collection.children.isEmpty()) {
            Snackbar.make(requireView(), "Ничего не найдено по запросу: $query", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun handleSearchError(error: Error) {
        val errorMessage = when (error) {
            is NetworkError -> "Ошибка сети. Проверьте подключение к интернету"
            is RemoteError -> "Ошибка сервера"
            else -> "Ошибка поиска"
        }
        Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_SHORT).show()
    }

    private fun updateSearchResults(results: List<PlacemarkData>) {
        // Результаты уже добавлены на карту в handleSearchResponse
    }

    private fun addPlacemark(point: Point, data: PlacemarkData) {
        val collection = mapObjects ?: return
        try {
            val imageResource = if (data.isVetClinic) R.drawable.ic_map else R.drawable.ic_map

            val placemark = collection.addPlacemark(point).apply {
                setIcon(ImageProvider.fromResource(requireContext(), imageResource))
                setIconStyle(IconStyle().apply {
                    scale = 0.8f
                    zIndex = 10f
                })
                userData = data
            }

            placemark.addTapListener { mapObject, _ ->
                val placemarkData = mapObject.userData as? PlacemarkData
                placemarkData?.let {
                    showPlaceInfo(it)
                }
                true
            }

            placemarks.add(placemark)
        } catch (e: Exception) {
            Log.e("MapFragment", "Ошибка добавления метки", e)
        }
    }

    private fun showPlaceInfo(data: PlacemarkData) {
        try {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val view = layoutInflater.inflate(R.layout.bottom_sheet_place_info, null)
            bottomSheetDialog.setContentView(view)

            val tvPlaceType = view.findViewById<TextView>(R.id.tvPlaceType)
            val tvPlaceName = view.findViewById<TextView>(R.id.tvPlaceName)
            val tvAddress = view.findViewById<TextView>(R.id.tvAddress)
            val tvOpenStatus = view.findViewById<TextView>(R.id.tvOpenStatus)
            val layoutRating = view.findViewById<LinearLayout>(R.id.layoutRating)
            val tvRating = view.findViewById<TextView>(R.id.tvRating)
            val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
            val tvRatingsCount = view.findViewById<TextView>(R.id.tvRatingsCount)
            val layoutWorkingHours = view.findViewById<LinearLayout>(R.id.layoutWorkingHours)
            val tvWorkingHours = view.findViewById<TextView>(R.id.tvWorkingHours)
            val layoutPhones = view.findViewById<LinearLayout>(R.id.layoutPhones)
            val containerPhones = view.findViewById<LinearLayout>(R.id.containerPhones)
            val layoutWebsite = view.findViewById<LinearLayout>(R.id.layoutWebsite)
            val tvWebsite = view.findViewById<TextView>(R.id.tvWebsite)
            val btnCall = view.findViewById<MaterialButton>(R.id.btnCall)
            val btnRoute = view.findViewById<MaterialButton>(R.id.btnRoute)

            tvPlaceType.text = if (data.isVetClinic) "Ветеринарная клиника" else "Зоомагазин"
            tvPlaceName.text = data.name
            tvAddress.text = data.address

            tvOpenStatus.text = "Часы работы неизвестны"
            tvOpenStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            layoutRating.visibility = View.GONE
            layoutWorkingHours.visibility = View.GONE
            layoutPhones.visibility = View.GONE
            layoutWebsite.visibility = View.GONE
            btnCall.visibility = View.GONE

            btnRoute.setOnClickListener {
                val uri = Uri.parse("yandexnavi://build_route?lat_to=${data.point.latitude}&lon_to=${data.point.longitude}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(intent)
                } else {
                    val mapsUri = Uri.parse("https://yandex.ru/maps/?pt=${data.point.longitude},${data.point.latitude}&z=15")
                    val mapsIntent = Intent(Intent.ACTION_VIEW, mapsUri)
                    startActivity(mapsIntent)
                }
            }

            bottomSheetDialog.show()
        } catch (e: Exception) {
            Log.e("MapFragment", "Ошибка показа информации о месте", e)
        }
    }

    private fun clearPlacemarks() {
        mapObjects?.clear()
        placemarks.clear()
        viewModel.clearSearchResults()
    }

    private fun showCity(cityPoint: Point, zoom: Float = 12.0f) {
        try {
            mapView.map.move(
                CameraPosition(cityPoint, zoom, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 0.5f),
                null
            )
        } catch (e: Exception) {
            Log.e("MapFragment", "Ошибка перемещения карты", e)
        }
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {
        val location = userLocationView.arrow.geometry
        viewModel.updateUserLocation(location)

        try {
            userLocationView.arrow.setIcon(
                ImageProvider.fromResource(requireContext(), R.drawable.ic_map)
            )
            userLocationView.accuracyCircle.fillColor = Color.argb(50, 76, 175, 80)
        } catch (e: Exception) {
            Log.e("MapFragment", "Ошибка установки иконки местоположения", e)
        }
    }

    override fun onObjectRemoved(userLocationView: UserLocationView) {}

    override fun onObjectUpdated(userLocationView: UserLocationView, event: ObjectEvent) {
        val location = userLocationView.arrow.geometry
        viewModel.updateUserLocation(location)
    }

    override fun onStart() {
        super.onStart()
        try {
            if (::mapView.isInitialized) {
                MapKitFactory.getInstance().onStart()
                mapView.onStart()
            }
        } catch (e: Exception) {
            Log.e("MapFragment", "Ошибка в onStart", e)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            if (::mapView.isInitialized) {
                mapView.onStop()
                MapKitFactory.getInstance().onStop()
            }
        } catch (e: Exception) {
            Log.e("MapFragment", "Ошибка в onStop", e)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = MapFragment()
    }
}